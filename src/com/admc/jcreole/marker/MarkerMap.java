/*
 * Copyright 2011 Axis Data Management Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.admc.jcreole.marker;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.admc.jcreole.CreoleParseException;

public class MarkerMap extends HashMap<Integer, BufferMarker> {
    private static Log log = LogFactory.getLog(MarkerMap.class);
private String[] classz = { "alpha", "beta", "gamma", "delta", "mu", "nu", "omicron" };
int nextOne = 0;

    public String apply(StringBuilder sb) {
        int offset = 0;
        BufferMarker marker;
        List<Integer> markerOffsets = new ArrayList<Integer>();
        String idString;
        int id;
        while ((offset = sb.indexOf("\u001a", offset)) > -1) {
            // Unfortunately StringBuilder has no indexOf(char).
            // We could make do StringBuilder.toString().indexOf(char), but
            // that's a pretty expensive copy operation.
            markerOffsets.add(Integer.valueOf(offset));
            if (sb.length() < offset + 4)
                throw new CreoleParseException(
                        "Marking too close to end of output");
            idString = sb.substring(offset + 1, offset + 5);
            id = Integer.parseInt(idString, 16);
            marker = get(Integer.valueOf(id));
            if (marker == null)
                throw new IllegalStateException("Lost marker with id " + id);
            marker.setContext(sb, offset);
            offset += 5;  // Move past the marker that we just found
        }
        List<BufferMarker> sortedMarkers = new ArrayList(values());
        Collections.sort(sortedMarkers);
        Collections.reverse(sortedMarkers);
        log.warn(Integer.toString(markerOffsets.size())
                + " markings: " + markerOffsets);
        if (markerOffsets.size() != sortedMarkers.size())
            throw new IllegalStateException(
                    "Markings/markers mismatch.  " + markerOffsets.size()
                    + " markings found, but there are " + size()
                    + " markers");
        // Can not run insert() until after the markers have been sorted.
        if (size() > 0) {
            StringBuilder markerReport = new StringBuilder();
            for (BufferMarker mer : sortedMarkers) {
//if (mer instanceof TagMarker) ((TagMarker) mer).add(classz[nextOne++]);
                if (markerReport.length() > 0) markerReport.append(", ");
                markerReport.append(mer.getIdString()
                        + '@' + mer.getOffset());
                mer.updateBuffer();
            }
            log.warn("MARKERS:  " + markerReport.toString());
        }
        return sb.toString();
    }
}
