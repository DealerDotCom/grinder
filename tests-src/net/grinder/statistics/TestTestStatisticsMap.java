// Copyright (C) 2001, 2002, 2003 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.statistics;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.grinder.common.StubTest;
import net.grinder.common.Test;


/**
 * Unit test case for <code>TestStatisticsMap</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see TestStatisticsMap
 */
public class TestTestStatisticsMap extends TestCase {

  public static void main(String[] args) {
    TestRunner.run(TestTestStatisticsMap.class);
  }

  public TestTestStatisticsMap(String name) {
    super(name);
  }

  private final Test m_test0 = new StubTest(0, "");
  private final Test m_test1 = new StubTest(1, "");
  private TestStatistics m_statistics0;
  private TestStatistics m_statistics1;
  private StatisticsIndexMap.LongIndex m_index;

  protected void setUp() throws Exception {
    final TestStatisticsFactory factory = TestStatisticsFactory.getInstance();

    m_statistics0 = factory.create();
    m_statistics1 = factory.create();

    m_index = StatisticsIndexMap.getInstance().getIndexForLong("userLong0");

    m_statistics0.addValue(m_index, 10);
  }

  public void testPut() throws Exception {

    final TestStatisticsMap map = new TestStatisticsMap();
    assertEquals(0, map.size());

    map.put(m_test0, m_statistics0);
    assertEquals(1, map.size());

    map.put(m_test0, m_statistics1);
    assertEquals(1, map.size());

    map.put(m_test1, m_statistics1);
    assertEquals(2, map.size());
  }

  public void testEquals() throws Exception {

    final TestStatisticsMap map0 = new TestStatisticsMap();
    final TestStatisticsMap map1 = new TestStatisticsMap();

    assertEquals(map0, map0);
    assertEquals(map0, map1);

    map0.put(m_test0, m_statistics0);
    assertTrue(!map0.equals(map1));

    map1.put(m_test1, m_statistics0);
    assertTrue(!map0.equals(map1));

    map0.put(m_test1, m_statistics0);
    map1.put(m_test0, m_statistics0);
    assertEquals(map0, map0);
    assertEquals(map0, map1);

    map1.put(m_test0, m_statistics1);
    assertTrue(!map0.equals(map1));
  }

  public void testGetDelta() throws Exception   {

    final TestStatisticsMap map0 = new TestStatisticsMap();
    map0.put(m_test0, m_statistics0);

    // map0 is now {(Test 0 (), RawStatistics = {10})}
    // snap shot is not set.

    final TestStatisticsMap map1 = map0.getDelta(false);
    assertEquals(map0, map1);

    // map0 is {(Test 0 (), RawStatistics = {10})}
    // snap shot is not set.

    final TestStatisticsMap map2 = map0.getDelta(true);
    assertEquals(map0, map1);
    assertEquals(map0, map2);

    // map0 is {(Test 0 (), RawStatistics = {10})}
    // snap shot is {(Test 0 (), RawStatistics = {10})}.

    final TestStatisticsMap map3 = map0.getDelta(false);
    assertTrue(!map0.equals(map3));
    assertEquals(map0.size(), map3.size());

    m_statistics0.add(m_statistics0);
    map0.put(m_test1, m_statistics1);

    // map0 is {(Test 0 (), RawStatistics = {20}), (Test 1 (), RawStatistics = {0})}
    // snap shot is {(Test 0 (), RawStatistics = {10})}.

    final TestStatisticsMap map4 = map0.getDelta(true);

    // map0 is {(Test 0 (), RawStatistics = {20}), (Test 1 (), RawStatistics = {0})}
    // snap shot is  {(Test 0 (), RawStatistics = {20}), (Test 1 (), RawStatistics = {0})}

    assertEquals(2, map4.size());
    final TestStatisticsMap.Iterator iterator = map4.new Iterator();

    final TestStatisticsMap.Pair first = iterator.next();
    assertEquals(0, first.getTest().getNumber());
    assertEquals(10, first.getStatistics().getValue(m_index));

    final TestStatisticsMap.Pair second = iterator.next();
    assertEquals(1, second.getTest().getNumber());
    assertEquals(0, second.getStatistics().getValue(m_index));
  }

  public void testIteratorAndOrder() throws Exception {

    final TestStatisticsMap map = new TestStatisticsMap();

    final TestStatisticsMap.Iterator iterator1 = map.new Iterator();
    assertTrue(!iterator1.hasNext());

    map.put(m_test1, m_statistics1);

    final TestStatisticsMap.Iterator iterator2 = map.new Iterator();
    assertTrue(iterator2.hasNext());
    assertTrue(!iterator1.hasNext());

    final TestStatisticsMap.Pair pair1 = iterator2.next();
    assertTrue(!iterator2.hasNext());
    assertEquals(m_test1, pair1.getTest());
    assertEquals(m_statistics1, pair1.getStatistics());

    map.put(m_test0, m_statistics0);

    final TestStatisticsMap.Iterator iterator3 = map.new Iterator();
    assertTrue(iterator3.hasNext());

    final TestStatisticsMap.Pair pair2 = iterator3.next();
    assertTrue(iterator3.hasNext());
    assertEquals(m_test0, pair2.getTest());
    assertEquals(m_statistics0, pair2.getStatistics());

    final TestStatisticsMap.Pair pair3 = iterator3.next();
    assertTrue(!iterator3.hasNext());
    assertEquals(m_test1, pair3.getTest());
    assertEquals(m_statistics1, pair3.getStatistics());

    try {
      iterator3.next();
      fail("Expected a NoSuchElementException");
    }
    catch (java.util.NoSuchElementException e) {
    }
  }

  public void testSerialisation() throws Exception {

    final TestStatisticsMap original0 = new TestStatisticsMap();
    original0.put(m_test0, m_statistics0);
    original0.put(m_test1, m_statistics0);

    final TestStatisticsMap original1 = new TestStatisticsMap();

    final ByteArrayOutputStream byteOutputStream =
      new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream =
      new ObjectOutputStream(byteOutputStream);

    objectOutputStream.writeObject(original0);
    objectOutputStream.writeObject(original1);

    objectOutputStream.close();

    final ObjectInputStream objectInputStream =
      new ObjectInputStream(
	new ByteArrayInputStream(byteOutputStream.toByteArray()));

    final TestStatisticsMap received0 =
      (TestStatisticsMap)objectInputStream.readObject();

    final TestStatisticsMap received1 =
      (TestStatisticsMap)objectInputStream.readObject();

    assertEquals(original0, received0);
    assertEquals(original1, received1);
  }	
}
