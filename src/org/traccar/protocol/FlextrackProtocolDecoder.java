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

import java.net.SocketAddress;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class FlextrackProtocolDecoder extends BaseProtocolDecoder {

    public FlextrackProtocolDecoder(FlextrackProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_LOGON = new PatternBuilder()
            .number("(-?d+),")                   // index
            .text("LOGON,")
            .number("(d+),")                     // node id
            .number("(d+)")                      // iccid
            .compile();

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(-?d+),")                   // index
            .text("UNITSTAT,")
            .number("(dddd)(dd)(dd),")           // date
            .number("(dd)(dd)(dd),")             // time
            .number("d+,")                       // node id
            .number("([NS])(d+).(d+.d+),")       // latitude
            .number("([EW])(d+).(d+.d+),")       // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // satellites
            .number("(d+),")                     // battery
            .number("(-?d+),")                   // gsm
            .number("(x+),")                     // state
            .number("(ddd)")                     // mcc
            .number("(dd),")                     // mnc
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // hdop
            .number("(x+),")                     // cell
            .number("d+,")                       // gps fix time
            .number("(x+),")                     // lac
            .number("(d+)")                      // odometer
            .compile();

    private void sendAcknowledgement(Channel channel, String index) {
        if (channel != null) {
            channel.write(index + ",ACK\r");
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.contains("LOGON")) {

            Parser parser = new Parser(PATTERN_LOGON, sentence);
            if (!parser.matches()) {
                return null;
            }

            sendAcknowledgement(channel, parser.next());

            String id = parser.next();
            String iccid = parser.next();

            if (!identify(iccid, channel, null, false) && !identify(id, channel)) {
                return null;
            }

        } else if (sentence.contains("UNITSTAT") && hasDeviceId()) {

            Parser parser = new Parser(PATTERN, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            sendAcknowledgement(channel, parser.next());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            position.setValid(true);
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
            position.setCourse(parser.nextInt());

            position.set(Event.KEY_SATELLITES, parser.nextInt());
            position.set(Event.KEY_BATTERY, parser.nextInt());
            position.set(Event.KEY_GSM, parser.nextInt());
            position.set(Event.KEY_STATUS, parser.nextInt(16));
            position.set(Event.KEY_MCC, parser.nextInt());
            position.set(Event.KEY_MNC, parser.nextInt());

            position.setAltitude(parser.nextInt());

            position.set(Event.KEY_HDOP, parser.nextInt() * 0.1);
            position.set(Event.KEY_CID, parser.nextInt(16));
            position.set(Event.KEY_LAC, parser.nextInt(16));
            position.set(Event.KEY_ODOMETER, parser.nextInt());

            return position;
        }

        return null;
    }

}
