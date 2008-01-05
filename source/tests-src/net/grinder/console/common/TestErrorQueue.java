// Copyright (C) 2004 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.common;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomObjectFactory;
import net.grinder.testutility.RandomStubFactory;


/**
 *  Unit test case for {@link ErrorQueue}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestErrorQueue extends TestCase {

  public void testErrorQueue() throws Exception {

    final ErrorQueue errorQueue = new ErrorQueue();

    final RandomObjectFactory randomObjectFactory = new RandomObjectFactory();

    final Method[] methods = ErrorQueue.class.getMethods();

    final List callData = new ArrayList();

    // Call without delegate.
    for (int i = 0; i < methods.length; ++i) {
      final Method method = methods[i];

      if (method.getName().startsWith("handle")) {
        final Object[] parameters =
          randomObjectFactory.generateParameters(method.getParameterTypes());

        callData.add(new CallData(method, parameters, null));

        method.invoke(errorQueue, parameters);
      }
    }

    // Set delegate and assert that it gets the pending events.
    final RandomStubFactory delegateErrorHandlerStubFactory =
      new RandomStubFactory(ErrorHandler.class);
    final ErrorHandler delegateErrorHandler =
      (ErrorHandler)delegateErrorHandlerStubFactory.getStub();

    errorQueue.setErrorHandler(delegateErrorHandler);
    
    final Iterator iterator = callData.iterator();

    while (iterator.hasNext()) {
      final CallData data = (CallData)iterator.next();

      delegateErrorHandlerStubFactory.assertSuccess(data.getMethodName(),
                                                    data.getParameters());
    }

    delegateErrorHandlerStubFactory.assertNoMoreCalls();

    // Delegate should get all new pending events.
    for (int i = 0; i < methods.length; ++i) {
      final String methodName = methods[i].getName();

      if (methodName.startsWith("handle")) {
        final Object[] parameters =
          randomObjectFactory.generateParameters(
            methods[i].getParameterTypes());

        methods[i].invoke(errorQueue, parameters);

        delegateErrorHandlerStubFactory.assertSuccess(methodName, parameters);
      }
    }

    delegateErrorHandlerStubFactory.assertNoMoreCalls();

    // Set no delegate, assert last delegate gets no new events.
    errorQueue.setErrorHandler(null);

    for (int i = 0; i < methods.length; ++i) {
      final Method method = methods[i];

      if (method.getName().startsWith("handle")) {
        final Object[] parameters =
          randomObjectFactory.generateParameters(method.getParameterTypes());

        callData.add(new CallData(method, parameters, null));

        method.invoke(errorQueue, parameters);
      }
    }

    delegateErrorHandlerStubFactory.assertNoMoreCalls();
  }
}
