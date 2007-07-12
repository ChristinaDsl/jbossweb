/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.comet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The CometEvent interface.
 * 
 * @author Filip Hanik
 * @author Remy Maucherat
 */
public interface CometEvent {

    /**
     * Enumeration describing the major events that the container can invoke 
     * the CometProcessor event() method with:
     * <ul>
     * <li>BEGIN - will be called at the beginning 
     *  of the processing of the connection. It can be used to initialize any relevant 
     *  fields using the request and response objects. Between the end of the processing 
     *  of this event, and the beginning of the processing of the end or error events,
     *  it is possible to use the response object to write data on the open connection.
     *  Note that the response object and dependent OutputStream and Writer are  
     *  not synchronized, so when they are accessed by multiple threads adequate
     *  synchronization is needed. After processing the initial event, the request 
     *  is considered to be committed.</li>
     * <li>READ - This indicates that input data is available, and that at least one 
     *  read can be made without blocking. The available and ready methods of the InputStream or
     *  Reader may be used to determine if there is a risk of blocking: the servlet
     *  must continue reading while data is reported available. When encountering a read error, 
     *  the servlet should report it by propagating the exception properly. Throwing 
     *  an exception will cause the error event to be invoked, and the connection 
     *  will be closed. 
     *  Alternately, it is also possible to catch any exception, perform clean up
     *  on any data structure the servlet may be using, and using the close method
     *  of the event. It is not allowed to attempt reading data from the request 
     *  object outside of the processing of this event, unless the suspend() method
     *  has been used.</li>
     * <li>END - End may be called to end the processing of the request. Fields that have
     *  been initialized in the begin method should be reset. After this event has
     *  been processed, the request and response objects, as well as all their dependent
     *  objects will be recycled and used to process other requests.</li>
     * <li>ERROR - Error will be called by the container in the case where an IO exception
     *  or a similar unrecoverable error occurs on the connection. Fields that have
     *  been initialized in the begin method should be reset. After this event has
     *  been processed, the request and response objects, as well as all their dependent
     *  objects will be recycled and used to process other requests.</li>
     * <li>EVENT - Event will be called by the container after the resume() method is called,
     *  during which any operations can be performed, including closing the Comet connection
     *  using the close() method.</li>
     * <li>WRITE - Write is sent if the servlet is using the ready method. This means that 
     *  the connection is ready to receive data to be written out. This event will never
     *  be received if the servlet is not using the ready() method, or if the ready() 
     *  method always returns true.</li>
     * </ul>
     */
    public enum EventType {BEGIN, READ, END, ERROR, WRITE, EVENT}
    
    
    /**
     * Enumeration containing event sub categories.
     * <br>
     * END events sub types:
     * <ul>
     * <li>WEBAPP_RELOAD - the web application is being reloaded</li>
     * <li>SERVER_SHUTDOWN - the server is shutting down</li>
     * <li>SESSION_END - the servlet ended the session</li>
     * </ul>
     * ERROR events sub types:
     * <ul>
     * <li>TIMEOUT - the connection timed out; note that this ERROR type is not fatal, and
     *   the connection will not be closed unless the servlet uses the close method of the event</li>
     * <li>CLIENT_DISCONNECT - the client connection was closed</li>
     * <li>IOEXCEPTION - an IO exception occurred, such as invalid content, for example, an invalid chunk block</li>
     * </ul>
     */
    public enum EventSubType { TIMEOUT, CLIENT_DISCONNECT, IOEXCEPTION, WEBAPP_RELOAD, SERVER_SHUTDOWN, SESSION_END }
    
    
    /**
     * Returns the HttpServletRequest.
     * 
     * @return HttpServletRequest
     */
    public HttpServletRequest getHttpServletRequest();
    
    /**
     * Returns the HttpServletResponse.
     * 
     * @return HttpServletResponse
     */
    public HttpServletResponse getHttpServletResponse();
    
    /**
     * Returns the event type.
     * 
     * @return EventType
     * @see #EventType
     */
    public EventType getEventType();
    
    /**
     * Returns the sub type of this event.
     * 
     * @return EventSubType
     * @see #EventSubType
     */
    public EventSubType getEventSubType();

    /**
     * Ends the request, which marks the end of the comet session. This will send 
     * back to the client a notice that the server has no more data to send 
     * as part of this request. If this method is called from a Tomcat provided thread
     * (during the processing of an event), the container will not call an END event.
     * If this method is called asynchronously, an END event will be sent to the 
     * servlet (note that this event will be sent whenever another event would have
     * been sent, such as a READ or ERROR/TIMEOUT event).
     * 
     * @throws IOException if an IO exception occurs
     */
    public void close() throws IOException;

    /**
     * This method sets the timeout in milliseconds of idle time on the connection.
     * The timeout is reset every time data is received from the connection or data is flushed
     * using <code>response.flushBuffer()</code>. If a timeout occurs, the 
     * servlet will receive an ERROR/TIMEOUT event which will not result in automatically closing
     * the event (the event may be closed using the close() method).
     * 
     * @param timeout The timeout in milliseconds for this connection, must be a positive value, larger than 0
     */
    public void setTimeout(int timeout);

    /**
     * Returns true when data may be written to the connection (the flag becomes false 
     * when the client is unable to accept data fast enough). When the flag becomes false, 
     * the servlet must stop writing data. If there's an attempt to flush additional data 
     * to the client and data still cannot be written immediately, an IOException will be 
     * thrown. If calling this method returns false, it will also 
     * request notification when the connection becomes available for writing again, and the  
     * servlet will receive a write event.
     * <br>
     * Note: If the servlet is not using ready, and is writing its output inside the
     * container threads, using this method is not mandatory, but any incomplete writes will be
     * performed again in blocking mode.
     * 
     * @return boolean true if data can be written 
     */
    public boolean ready();

    /**
     * Suspend processing of the connection until the configured timeout occurs, 
     * or resume() is called. In practice, this means the servlet will no longer 
     * receive read events. Reading should always be performed synchronously in 
     * the Tomcat threads unless the connection has been suspended.
     */
    public void suspend();

    /**
     * Resume will cause the servlet container to send a generic event 
     * to the servlet, where the request can be processed synchronously 
     * (for example, it is possible to use this to complete the request after 
     * some asynchronous processing is done). This also resumes read events 
     * if they have been disabled using suspend. It is then possible to call suspend 
     * again later. It is also possible to call resume without calling suspend before.
     */
    public void resume();

}
