package de.uka.ilkd.key.symbolic_execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.uka.ilkd.key.java.Expression;
import de.uka.ilkd.key.java.PositionInfo;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.SourceElement;
import de.uka.ilkd.key.java.reference.MethodReference;
import de.uka.ilkd.key.java.statement.BranchStatement;
import de.uka.ilkd.key.java.statement.LoopStatement;
import de.uka.ilkd.key.java.statement.MethodBodyStatement;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.op.IProgramVariable;
import de.uka.ilkd.key.logic.op.ProgramMethod;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.NodeInfo;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionBranchCondition;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionBranchNode;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionElement;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionLoopCondition;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionLoopNode;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionMethodCall;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionMethodReturn;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionNode;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionStartNode;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionStateNode;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionStatement;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionTermination;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionVariable;

/**
 * Allows to read XML files which contains an symbolic execution tree
 * written via an {@link ExecutionNodeWriter}.
 * @author Martin Hentschel
 * @see ExecutionNodeWriter
 */
public class ExecutionNodeReader {
   /**
    * Reads the given {@link File}.
    * @param file The {@link File} to read.
    * @return The root of the read symbolic execution tree.
    * @throws ParserConfigurationException Occurred Exception.
    * @throws SAXException Occurred Exception.
    * @throws IOException Occurred Exception.
    */
   public IExecutionNode read(File file) throws ParserConfigurationException, SAXException, IOException {
      return read(new FileInputStream(file));
   }
   
   /**
    * Reads from the given {@link InputStream} and closes it.
    * @param in The {@link InputStream} to read from.
    * @return The root of the read symbolic execution tree.
    * @throws ParserConfigurationException Occurred Exception.
    * @throws SAXException Occurred Exception.
    * @throws IOException Occurred Exception.
    */
   public IExecutionNode read(InputStream in) throws ParserConfigurationException, SAXException, IOException {
      if (in != null) {
         try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            SEDSAXHandler handler = new SEDSAXHandler();
            saxParser.parse(in, handler);
            return handler.getRoot();
         }
         finally {
            in.close();
         }
      }
      else {
         return null;
      }
   }
   
   /**
    * {@link DefaultHandler} implementation used in {@link ExecutionNodeReader#read(InputStream)}.
    * @author Martin Hentschel
    */
   private class SEDSAXHandler extends DefaultHandler {
      /**
       * The root of the read symbolic execution tree.
       */
      private IExecutionNode root;

      /**
       * The parent hierarchy filled by {@link #startElement(String, String, String, Attributes)}
       * and emptied by {@link #endElement(String, String, String)}.
       */
      private Deque<AbstractKeYlessExecutionNode> parentNodeStack = new LinkedList<AbstractKeYlessExecutionNode>();

      /**
       * The parent hierarchy of {@link IExecutionVariable} filled by {@link #startElement(String, String, String, Attributes)}
       * and emptied by {@link #endElement(String, String, String)}. 
       */
      private Deque<KeYlessVariable> parentVariableStack = new LinkedList<KeYlessVariable>();
      
      /**
       * {@inheritDoc}
       */
      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
         AbstractKeYlessExecutionNode parent = parentNodeStack.peekFirst();
         if (isVariable(uri, localName, qName)) {
            KeYlessVariable parentVariable = parentVariableStack.peekFirst();
            KeYlessVariable variable = createVariable(parentVariable, uri, localName, qName, attributes);
            if (parentVariable != null) {
               parentVariable.addChildVariable(variable);
            }
            else {
               if (parent instanceof AbstractKeYlessStateNode<?>) {
                  ((AbstractKeYlessStateNode<?>)parent).addVariable(variable);
               }
               else {
                  throw new SAXException("Can't add variable to parent executio node.");
               }
            }
            parentVariableStack.addFirst(variable);
         }
         else {
            AbstractKeYlessExecutionNode child = createExecutionNode(parent, uri, localName, qName, attributes);
            if (root == null) {
               root = child;
            }
            if (parent != null) {
               parent.addChild(child);
            }
            parentNodeStack.addFirst(child);
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
         if (isVariable(uri, localName, qName)) {
            parentVariableStack.removeFirst();
         }
         else {
            parentNodeStack.removeFirst();
         }
      }
      
      /**
       * Returns the root of the read symbolic execution tree.
       * @return The root of the read symbolic execution tree.
       */
      public IExecutionNode getRoot() {
         return root;
      }
   }
   
   /**
    * Checks if the currently parsed tag represents an {@link IExecutionVariable}.
    * @param uri The URI.
    * @param localName THe local name.
    * @param qName The qName.
    * @return {@code true} represents an {@link IExecutionVariable}, {@code false} is something else.
    */
   protected boolean isVariable(String uri, String localName, String qName) {
      return ExecutionNodeWriter.TAG_VARIABLE.equals(qName);
   }
   
   /**
    * Creates a new {@link IExecutionVariable} with the given content.
    * @param parentVariable The parent {@link IExecutionVariable}.
    * @param uri The URI.
    * @param localName THe local name.
    * @param qName The qName.
    * @param attributes The attributes.
    * @return The created {@link IExecutionVariable}.
    */
   public KeYlessVariable createVariable(IExecutionVariable parentVariable,
                                         String uri, 
                                         String localName, 
                                         String qName, 
                                         Attributes attributes) {
      return new KeYlessVariable(parentVariable, 
                                 isArrayIndex(attributes), 
                                 getArrayIndex(attributes), 
                                 getTypeString(attributes), 
                                 getValueString(attributes), 
                                 getName(attributes));
   }

   /**
    * Creates a new {@link IExecutionNode} with the given content.
    * @param parent The parent {@link IExecutionNode}.
    * @param uri The URI.
    * @param localName THe local name.
    * @param qName The qName.
    * @param attributes The attributes.
    * @return The created {@link IExecutionNode}.
    * @throws SAXException Occurred Exception.
    */
   protected AbstractKeYlessExecutionNode createExecutionNode(IExecutionNode parent, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if (ExecutionNodeWriter.TAG_BRANCH_CONDITION.equals(qName)) {
         return new KeYlessBranchCondition(parent, getName(attributes));
      }
      else if (ExecutionNodeWriter.TAG_BRANCH_NODE.equals(qName)) {
         return new KeYlessBranchNode(parent, getName(attributes));
      }
      else if (ExecutionNodeWriter.TAG_LOOP_CONDITION.equals(qName)) {
         return new KeYlessLoopCondition(parent, getName(attributes));
      }
      else if (ExecutionNodeWriter.TAG_LOOP_NODE.equals(qName)) {
         return new KeYlessLoopNode(parent, getName(attributes));
      }
      else if (ExecutionNodeWriter.TAG_METHOD_CALL.equals(qName)) {
         return new KeYlessMethodCall(parent, getName(attributes));
      }
      else if (ExecutionNodeWriter.TAG_METHOD_RETURN.equals(qName)) {
         return new KeYlessMethodReturn(parent, getName(attributes), getNameIncludingReturnValue(attributes));
      }
      else if (ExecutionNodeWriter.TAG_START_NODE.equals(qName)) {
         return new KeYlessStartNode(getName(attributes));
      }
      else if (ExecutionNodeWriter.TAG_STATEMENT.equals(qName)) {
         return new KeYlessStatement(parent, getName(attributes));
      }
      else if (ExecutionNodeWriter.TAG_TERMINATION.equals(qName)) {
         return new KeYlessTermination(parent, getName(attributes), isExceptionalTermination(attributes));
      }
      else {
         throw new SAXException("Unknown tag \"" + qName + "\".");
      }
   }
   
   /**
    * Returns the name value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getName(Attributes attributes) {
      return attributes.getValue(ExecutionNodeWriter.ATTRIBUTE_NAME);
   }
   
   /**
    * Returns the name value including return value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getNameIncludingReturnValue(Attributes attributes) {
      return attributes.getValue(ExecutionNodeWriter.ATTRIBUTE_NAME_INCLUDING_RETURN_VALUE);
   }
   
   /**
    * Returns the exceptional termination value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isExceptionalTermination(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(ExecutionNodeWriter.ATTRIBUTE_EXCEPTIONAL_TERMINATION));
   }

   /**
    * Returns the value string value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getValueString(Attributes attributes) {
      return attributes.getValue(ExecutionNodeWriter.ATTRIBUTE_VALUE_STRING);
   }

   /**
    * Returns the type string value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getTypeString(Attributes attributes) {
      return attributes.getValue(ExecutionNodeWriter.ATTRIBUTE_TYPE_STRING);
   }

   /**
    * Returns the array index value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected int getArrayIndex(Attributes attributes) {
      return Integer.parseInt(attributes.getValue(ExecutionNodeWriter.ATTRIBUTE_ARRAY_INDEX));
   }

   /**
    * Returns the is array index value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isArrayIndex(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(ExecutionNodeWriter.ATTRIBUTE_IS_ARRAY_INDEX));
   }
   
   /**
    * An abstract implementation of {@link IExecutionElement} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static abstract class AbstractKeYlessExecutionElement implements IExecutionElement {
      /**
       * The name.
       */
      private String name;
      
      /**
       * Constructor.
       * @param name The name of this node.
       */
      public AbstractKeYlessExecutionElement(String name) {
         this.name = name;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Services getServices() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Proof getProof() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Node getProofNode() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public NodeInfo getProofNodeInfo() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getName() {
         return name;
      }
      
      /**
       * {@inheritDoc}
       */
      @Override
      public String toString() {
         return getElementType() + " " + getName();
      }
   }
   
   /**
    * An abstract implementation of {@link IExecutionNode} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static abstract class AbstractKeYlessExecutionNode extends AbstractKeYlessExecutionElement implements IExecutionNode {
      /**
       * The parent {@link IExecutionNode}.
       */
      private IExecutionNode parent;
      
      /**
       * The children.
       */
      private List<IExecutionNode> children = new LinkedList<IExecutionNode>();

      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public AbstractKeYlessExecutionNode(IExecutionNode parent, String name) {
         super(name);
         this.parent = parent;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public IExecutionNode getParent() {
         return parent;
      }
      
      /**
       * Adds the given child.
       * @param child The child to add.
       */
      public void addChild(IExecutionNode child) {
         children.add(child);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public IExecutionNode[] getChildren() {
         return children.toArray(new IExecutionNode[children.size()]);
      }
   }
   
   /**
    * An implementation of {@link IExecutionLoopCondition} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessBranchCondition extends AbstractKeYlessExecutionNode implements IExecutionBranchCondition {
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public KeYlessBranchCondition(IExecutionNode parent, String name) {
         super(parent, name);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Branch Condition";
      }
   }

   /**
    * An implementation of {@link IExecutionStartNode} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessStartNode extends AbstractKeYlessExecutionNode implements IExecutionStartNode {
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public KeYlessStartNode(String name) {
         super(null, name);
      }
      
      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Start Node";
      }
   }
   
   /**
    * An implementation of {@link IExecutionTermination} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessTermination extends AbstractKeYlessExecutionNode implements IExecutionTermination {
      /**
       * Exceptional termination?
       */
      private boolean exceptionalTermination;
      
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       * @param exceptionalTermination Exceptional termination?
       */
      public KeYlessTermination(IExecutionNode parent, String name, boolean exceptionalTermination) {
         super(parent, name);
         this.exceptionalTermination = exceptionalTermination;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public IProgramVariable getExceptionVariable() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Sort getExceptionSort() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean isExceptionalTermination() {
         return exceptionalTermination;
      }
      
      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return isExceptionalTermination() ? "Exceptional Termination" : "Termination";
      }
   }

   /**
    * An abstract implementation of {@link IExecutionStateNode} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static abstract class AbstractKeYlessStateNode<S extends SourceElement> extends AbstractKeYlessExecutionNode implements IExecutionStateNode<S> {
      /**
       * The contained variables.
       */
      private List<IExecutionVariable> variables = new LinkedList<IExecutionVariable>();
      
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public AbstractKeYlessStateNode(IExecutionNode parent, String name) {
         super(parent, name);
      }
      
      /**
       * Adds the given {@link IExecutionVariable}.
       * @param variable The {@link IExecutionVariable} to add.
       */
      public void addVariable(IExecutionVariable variable) {
         variables.add(variable);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public S getActiveStatement() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public PositionInfo getActivePositionInfo() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public IExecutionVariable[] getVariables() {
         return variables.toArray(new IExecutionVariable[variables.size()]);
      }
   }
   
   /**
    * An implementation of {@link IExecutionBranchNode} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessBranchNode extends AbstractKeYlessStateNode<BranchStatement> implements IExecutionBranchNode {
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public KeYlessBranchNode(IExecutionNode parent, String name) {
         super(parent, name);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Branch Node";
      }
   }
   
   /**
    * An implementation of {@link IExecutionLoopCondition} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessLoopCondition extends AbstractKeYlessStateNode<LoopStatement> implements IExecutionLoopCondition {
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public KeYlessLoopCondition(IExecutionNode parent, String name) {
         super(parent, name);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Expression getGuardExpression() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public PositionInfo getGuardExpressionPositionInfo() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Loop Condition";
      }
   }

   /**
    * An implementation of {@link IExecutionLoopNode} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessLoopNode extends AbstractKeYlessStateNode<LoopStatement> implements IExecutionLoopNode {
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public KeYlessLoopNode(IExecutionNode parent, String name) {
         super(parent, name);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Loop Node";
      }
   }

   /**
    * An implementation of {@link IExecutionMethodCall} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessMethodCall extends AbstractKeYlessStateNode<MethodBodyStatement> implements IExecutionMethodCall {
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public KeYlessMethodCall(IExecutionNode parent, String name) {
         super(parent, name);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public MethodReference getMethodReference() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public ProgramMethod getProgramMethod() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Method Call";
      }
   }

   /**
    * An implementation of {@link IExecutionMethodReturn} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessMethodReturn extends AbstractKeYlessStateNode<SourceElement> implements IExecutionMethodReturn {
      /**
       * The name including the return value.
       */
      private String nameIncludingReturnValue;

      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       * @param nameIncludingReturnValue The name including the return value.
       */
      public KeYlessMethodReturn(IExecutionNode parent, String name, String nameIncludingReturnValue) {
         super(parent, name);
         this.nameIncludingReturnValue = nameIncludingReturnValue;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public IExecutionMethodCall getMethodCall() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getNameIncludingReturnValue() throws ProofInputException {
         return nameIncludingReturnValue;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Term getReturnValue() throws ProofInputException {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getFormatedReturnValue() throws ProofInputException {
         return null;
      }
      
      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Method Return";
      }
   }

   /**
    * An implementation of {@link IExecutionStatement} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessStatement extends AbstractKeYlessStateNode<SourceElement> implements IExecutionStatement {
      /**
       * Constructor.
       * @param parent The parent {@link IExecutionNode}.
       * @param name The name of this node.
       */
      public KeYlessStatement(IExecutionNode parent, String name) {
         super(parent, name);
      }
      
      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Statement";
      }
   }
   
   /**
    * An implementation of {@link IExecutionVariable} which is independent
    * from KeY and provides such only children and default attributes.
    * @author Martin Hentschel
    */
   public static class KeYlessVariable extends AbstractKeYlessExecutionElement implements IExecutionVariable {
      /**
       * The parent {@link IExecutionVariable} if available.
       */
      private IExecutionVariable parentVariable;
      
      /**
       * The is array flag.
       */
      private boolean isArrayIndex;

      /**
       * The array index.
       */
      private int arrayIndex;
      
      /**
       * The type string.
       */
      private String typeString;
      
      /**
       * The value string.
       */
      private String valueString;
      
      /**
       * The child variables.
       */
      private List<IExecutionVariable> childVariables = new LinkedList<IExecutionVariable>();
      
      /**
       * Constructor.
       * @param parentVariable The parent {@link IExecutionVariable} if available.
       * @param isArrayIndex The is array flag.
       * @param arrayIndex The array index.
       * @param typeString The type string.
       * @param valueString The value string.
       * @param name The name.
       */
      public KeYlessVariable(IExecutionVariable parentVariable, 
                             boolean isArrayIndex, 
                             int arrayIndex, 
                             String typeString, 
                             String valueString, 
                             String name) {
         super(name);
         this.parentVariable = parentVariable;
         this.isArrayIndex = isArrayIndex;
         this.arrayIndex = arrayIndex;
         this.typeString = typeString;
         this.valueString = valueString;
      }
      
      /**
       * Adds the given child {@link IExecutionVariable}.
       * @param variable The child {@link IExecutionVariable} to add.
       */
      public void addChildVariable(IExecutionVariable variable) {
         childVariables.add(variable);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getValueString() throws ProofInputException {
         return valueString;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getTypeString() {
         return typeString;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public IExecutionVariable getParentVariable() {
         return parentVariable;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public IExecutionVariable[] getChildVariables() throws ProofInputException {
         return childVariables.toArray(new IExecutionVariable[childVariables.size()]);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int getArrayIndex() {
         return arrayIndex;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean isArrayIndex() {
         return isArrayIndex;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public IProgramVariable getProgramVariable() {
         return null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Term getValue() throws ProofInputException {
         return null;
      }
      
      /**
       * {@inheritDoc}
       */
      @Override
      public String getElementType() {
         return "Variable";
      }
   }
}