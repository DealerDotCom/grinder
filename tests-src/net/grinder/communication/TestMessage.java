// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

package net.grinder.communication;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatisticsMap;


/**
 *  Unit test case for <code>Message</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestMessage extends TestCase {

  private static Random s_random = new Random();

  public TestMessage(String name) {
    super(name);
  }

  public void testSenderInformation() throws Exception {
    final Message message = new MyMessage();

    try {
      message.getSenderGrinderID();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }

    try {
      message.getSenderUniqueID();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }

    try {
      message.getSequenceNumber();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }

    message.setSenderInformation("grinderID", "uniqueID", 12345l);

    assertEquals("grinderID", message.getSenderGrinderID());
    assertEquals("uniqueID", message.getSenderUniqueID());
    assertEquals(12345l, message.getSequenceNumber());
  }

  private Message serialise(Message original) throws Exception {

    original.setSenderInformation("grinderID", "uniqueID",
                                  s_random.nextLong());

    final ByteArrayOutputStream byteOutputStream =
      new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream =
      new ObjectOutputStream(byteOutputStream);

    objectOutputStream.writeObject(original);
    objectOutputStream.close();

    final ObjectInputStream objectInputStream =
      new ObjectInputStream(
        new ByteArrayInputStream(byteOutputStream.toByteArray()));

    final Message received = (Message)objectInputStream.readObject();
    assertTrue(received != original);
    assertEquals(original, received);

    return received;
  }

  public void testSerialisation() throws Exception {
    serialise(new MyMessage());
  }

  public void testEquals() throws Exception {

    final Message m1 = new MyMessage();
    final Message m2 = new MyMessage();

    assertTrue("No uninitialised message is equal to another Message",
               !m1.equals(m2));

    m1.setSenderInformation("grinderID", "uniqueID", 12345l);

    assertTrue("No uninitialised message is equal to another Message",
               !m1.equals(m2));

    m2.setSenderInformation("grinderID2", "uniqueID", 12345l);

    assertEquals(
      "Initialised messages equal iff uniqueID and sequenceID equal",
      m1, m2);

    assertEquals("Reflexive", m2, m1);

    final Message m3 = new MyMessage();
    m3.setSenderInformation("grinderID3", "uniqueID", 12345l);

    assertEquals("Transitive", m2, m3);
    assertEquals("Transitive", m3, m1);

    m2.setSenderInformation("grinderID2", "uniqueID2", 12345l);

    assertTrue("Initialised messages equal iff uniqueID and sequenceID equal",
               !m1.equals(m2));

    m1.setSenderInformation("grinderID2", "uniqueID2", 12445l);
        
    assertTrue("Initialised messages equal iff uniqueID and sequenceID equal",
               !m1.equals(m2));
  }

  private static class MyMessage extends Message {
  }

  public void testCloseCommunicationMessage() throws Exception {
    final Message original = new CloseCommunicationMessage();
    final Message received = (CloseCommunicationMessage) serialise(original);

    assertEquals(original, received);
  }

  public void testRegisterStatisticsViewMessage() throws Exception {

    final StatisticsView statisticsView = new StatisticsView();
    statisticsView.add(new ExpressionView("One", "blah", "userLong0"));

    final RegisterStatisticsViewMessage original =
      new RegisterStatisticsViewMessage(statisticsView);

    assertEquals(1, original.getStatisticsView().getExpressionViews().length);

    final RegisterStatisticsViewMessage received =
      (RegisterStatisticsViewMessage) serialise(original);

    assertEquals(1, received.getStatisticsView().getExpressionViews().length);
    assertEquals(original.getStatisticsView().getExpressionViews()[0],
                 received.getStatisticsView().getExpressionViews()[0]);
  }

  public void testRegisterTestsMessage() throws Exception {

    final Collection c = new HashSet();

    final RegisterTestsMessage original = new RegisterTestsMessage(c);

    assertEquals(c, original.getTests());

    final RegisterTestsMessage received =
      (RegisterTestsMessage) serialise(original);

    assertEquals(original.getTests(), received.getTests());
  }

  public void testReportStatisticsMessage() throws Exception {

    final TestStatisticsMap statisticsDelta = new TestStatisticsMap();

    final ReportStatisticsMessage original =
      new ReportStatisticsMessage(statisticsDelta);

    assertEquals(statisticsDelta, original.getStatisticsDelta());

    final ReportStatisticsMessage received =
      (ReportStatisticsMessage) serialise(original);

    assertEquals(original.getStatisticsDelta(), received.getStatisticsDelta());
  }

  public void testReportStatusMessage() throws Exception {

    final ReportStatusMessage original =
      new ReportStatusMessage((short)1, (short)2, (short)3);

    assertEquals(1, original.getState());
    assertEquals(2, original.getNumberOfRunningThreads());
    assertEquals(3, original.getTotalNumberOfThreads());

    final ReportStatusMessage received =
      (ReportStatusMessage) serialise(original);

    assertEquals(1, received.getState());
    assertEquals(2, received.getNumberOfRunningThreads());
    assertEquals(3, received.getTotalNumberOfThreads());
  }

  public void testResetGrinderMessage() throws Exception {
    final Message original = new ResetGrinderMessage();
    final Message received = (ResetGrinderMessage) serialise(original);

    assertEquals(original, received);
  }

  public void testStartGrinderMessage() throws Exception {
    final Message original = new StartGrinderMessage();
    final Message received = (StartGrinderMessage) serialise(original);

    assertEquals(original, received);
  }

  public void testStopGrinderMessage() throws Exception {
    final Message original = new StopGrinderMessage();
    final Message received = (StopGrinderMessage) serialise(original);

    assertEquals(original, received);
  }
}
