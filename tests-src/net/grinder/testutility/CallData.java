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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.testutility;

import junit.framework.Assert;


/**
 *  Method call data.
 *
 * @author    Philip Aston
 */
public final class CallData extends Assert {
  private final String m_methodName;
  private final Object[] m_parameters;
  private final Object m_result;
  private final Throwable m_throwable;

  public CallData(String methodName, Object[] parameters, Object result) {
    m_methodName = methodName;
    m_parameters = parameters;
    m_result = result;
    m_throwable = null;
  }

  public CallData(String methodName, Object[] parameters,
                  Throwable throwable) {
    m_methodName = methodName;
    m_parameters = parameters;
    m_result = null;
    m_throwable = throwable;
  }

  public String getMethodName() {
    return m_methodName;
  }

  public Object[] getParameters() {
    return m_parameters;
  }

  public Class[] getParameterTypes() {
    if (m_parameters == null) {
      return new Class[0];
    };

    final Class[] types = new Class[m_parameters.length];

    for (int i=0; i<types.length; ++i) {
      types[i] = m_parameters[i].getClass();
    }

    return types;
  }

  public Object getResult() {
    assertNull(m_throwable);
    return m_result;
  }

  public Throwable getThrowable() {
    return m_throwable;
  }

  public String toString() {
    final StringBuffer result = new StringBuffer();

    result.append(getMethodName());
    result.append(CallRecorder.parametersToString(getParameters()));

    final Throwable throwable = getThrowable();

    if (throwable != null) {
      result.append(" threw " + throwable);
    }
    else {
      result.append(" returned " + getResult());
    }

    return result.toString();
  }
}
