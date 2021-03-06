/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class AutoFonFrameDecoder extends FrameDecoder {

    public static final int MSG_LOGIN = 0x10;
    public static final int MSG_LOCATION = 0x11;
    public static final int MSG_HISTORY = 0x12;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        // Check minimum length
        if (buf.readableBytes() < 12) {
            return null;
        }

        int length;
        switch (buf.getUnsignedByte(buf.readerIndex())) {
            case MSG_LOGIN:
                length = 12;
                break;
            case MSG_LOCATION:
                length = 78;
                break;
            case MSG_HISTORY:
                length = 257;
                break;
            default:
                length = 0;
                break;
        }

        // Check length and return buffer
        if (length != 0 && buf.readableBytes() >= length) {
            return buf.readBytes(length);
        }

        return null;
    }

}
