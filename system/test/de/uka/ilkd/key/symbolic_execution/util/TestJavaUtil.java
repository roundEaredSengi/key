package de.uka.ilkd.key.symbolic_execution.util;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests for {@link JavaUtil}.
 * @author Martin Hentschel
 */
public class TestJavaUtil extends TestCase {
   /**
    * Tests {@link JavaUtil#equalIgnoreWhiteSpace(String, String)}.
    */
   public void testEqualIgnoreWhiteSpace() {
      assertTrue(JavaUtil.equalIgnoreWhiteSpace(null, null));
      assertFalse(JavaUtil.equalIgnoreWhiteSpace("A", null));
      assertFalse(JavaUtil.equalIgnoreWhiteSpace("B", null));
      assertTrue(JavaUtil.equalIgnoreWhiteSpace("A", "A"));
      assertTrue(JavaUtil.equalIgnoreWhiteSpace("A B", "A B"));
      assertTrue(JavaUtil.equalIgnoreWhiteSpace("A B C", "A B C"));
      assertTrue(JavaUtil.equalIgnoreWhiteSpace("A    B    C", "A\nB\r\tC"));
      assertFalse(JavaUtil.equalIgnoreWhiteSpace("A B C", "A B C D"));
      assertFalse(JavaUtil.equalIgnoreWhiteSpace("A B C D", "A B C"));
      assertTrue(JavaUtil.equalIgnoreWhiteSpace("  A B C", "A B C\t\n"));
   }
   
   /**
    * Tests {@link JavaUtil#createLine(String, int)}
    */
   public void testCreateLine() {
      // Test line with one character
      assertEquals("", JavaUtil.createLine("#", -1));
      assertEquals("", JavaUtil.createLine("#", 0));
      assertEquals("-", JavaUtil.createLine("-", 1));
      assertEquals("AA", JavaUtil.createLine("A", 2));
      assertEquals("#####", JavaUtil.createLine("#", 5));
      // Test line with multiple characters
      assertEquals("ABABAB", JavaUtil.createLine("AB", 3));
      // Test null text
      assertEquals("nullnullnullnull", JavaUtil.createLine(null, 4));
   }
   
   /**
    * Tests {@link JavaUtil#encodeText(String)}
    */
   public void testEncodeText() {
      // Test null
      assertNull(JavaUtil.encodeText(null));
      // Test empty string
      assertEquals("", JavaUtil.encodeText(""));
      // Text XML tags
      assertEquals("&lt;hello&gt;world&lt;/hello&gt;", JavaUtil.encodeText("<hello>world</hello>"));
      // Test XML attributes
      assertEquals("&lt;hello a=&quot;A&quot; b=&apos;B&apos;&gt;world&lt;/hello&gt;", JavaUtil.encodeText("<hello a=\"A\" b='B'>world</hello>"));
      // Test XML entities
      assertEquals("&lt;hello a=&quot;A&quot; b=&apos;B&apos;&gt;&amp;lt;world&amp;gt;&lt;/hello&gt;", JavaUtil.encodeText("<hello a=\"A\" b='B'>&lt;world&gt;</hello>"));
   }
   
   /**
    * Tests {@link JavaUtil#isEmpty(Object[])}
    */
   public void testIsEmpty() {
      assertTrue(JavaUtil.isEmpty(null));
      assertTrue(JavaUtil.isEmpty(new String[] {}));
      assertFalse(JavaUtil.isEmpty(new String[] {"A"}));
      assertFalse(JavaUtil.isEmpty(new String[] {null}));
      assertFalse(JavaUtil.isEmpty(new String[] {"A", "B"}));
   }
   
   /**
    * Tests {@link JavaUtil#isTrimmedEmpty(String)}
    */
   public void testIsTrimmedEmpty() {
      assertTrue(JavaUtil.isTrimmedEmpty(null));
      assertTrue(JavaUtil.isTrimmedEmpty(""));
      assertTrue(JavaUtil.isTrimmedEmpty(" "));
      assertFalse(JavaUtil.isTrimmedEmpty(" A "));
   }
   
   /**
    * Tests {@link JavaUtil#equals(Object, Object)}
    */
   public void testEquals() {
      assertTrue(JavaUtil.equals(null, null));
      assertFalse(JavaUtil.equals(null, "A"));
      assertFalse(JavaUtil.equals("A", null));
      assertTrue(JavaUtil.equals("A", "A"));
      assertFalse(JavaUtil.equals("A", "B"));
      assertFalse(JavaUtil.equals("B", "A"));
      assertTrue(JavaUtil.equals("B", "B"));
   }
   
   /**
    * Tests {@link JavaUtil#count(Iterable, IFilter)}.
    */
   public void testCount() {
      // Create model
      List<String> list = new LinkedList<String>();
      list.add("A");
      list.add("B");
      list.add("A");
      list.add("C");
      list.add("B");
      list.add("A");
      // Test counts
      assertEquals(0, JavaUtil.count(null, null));
      assertEquals(0, JavaUtil.count(list, null));
      assertEquals(0, JavaUtil.count(null, new IFilter<String>() {
         @Override
         public boolean select(String element) {
            return false;
         }
      }));
      assertEquals(3, JavaUtil.count(list, new IFilter<String>() {
         @Override
         public boolean select(String element) {
            return "A".equals(element);
         }
      }));
      assertEquals(2, JavaUtil.count(list, new IFilter<String>() {
         @Override
         public boolean select(String element) {
            return "B".equals(element);
         }
      }));
      assertEquals(1, JavaUtil.count(list, new IFilter<String>() {
         @Override
         public boolean select(String element) {
            return "C".equals(element);
         }
      }));
      assertEquals(0, JavaUtil.count(list, new IFilter<String>() {
         @Override
         public boolean select(String element) {
            return "D".equals(element);
         }
      }));
   }

   /**
    * Tests for {@link JavaUtil#search(Iterable, IFilter)}.
    */
   public void testSearch() {
       List<String> collection = new LinkedList<String>();
       collection.add("A");
       collection.add("B");
       collection.add("C");
       collection.add("D");
       assertEquals("A", JavaUtil.search(collection, new IFilter<String>() {
          @Override
          public boolean select(String element) {
             return "A".equals(element);
          }
       }));
       assertEquals("B", JavaUtil.search(collection, new IFilter<String>() {
          @Override
          public boolean select(String element) {
             return "B".equals(element);
          }
       }));
       assertEquals("C", JavaUtil.search(collection, new IFilter<String>() {
          @Override
          public boolean select(String element) {
             return "C".equals(element);
          }
       }));
       assertEquals("D", JavaUtil.search(collection, new IFilter<String>() {
          @Override
          public boolean select(String element) {
             return "D".equals(element);
          }
       }));
       assertNull(JavaUtil.search(collection, new IFilter<String>() {
          @Override
          public boolean select(String element) {
             return "E".equals(element);
          }
       }));
       assertNull(JavaUtil.search(collection, null));
       assertNull(JavaUtil.search(null, new IFilter<String>() {
          @Override
          public boolean select(String element) {
             return "E".equals(element);
          }
       }));
   }
}