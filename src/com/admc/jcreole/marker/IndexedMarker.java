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

import java.util.List;
import com.admc.jcreole.Sections;

/**
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class IndexedMarker extends BufferMarker {
    private int targNum = -1;
    private String name;

    public IndexedMarker(int id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getTargNum() {
        return targNum;
    }

    public void setTargNum(int targNum) {
        this.targNum = targNum;
    }

    public void updateBuffer() {
        if (targNum < 0)
            throw new IllegalStateException(
                    "Can't generate Index Entry tag until targNum is set");
        super.updateBuffer();
        targetSb.insert(offset, targNum);
    }
}
