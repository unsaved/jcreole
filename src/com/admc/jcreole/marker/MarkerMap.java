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
import com.admc.jcreole.TagType;
import com.admc.jcreole.SectionHeading;

public class MarkerMap extends HashMap<Integer, BufferMarker> {
    private static Log log = LogFactory.getLog(MarkerMap.class);
    private List<SectionHeading> sectionHeadings;
    private Map<String, String> idToTextMap = new HashMap<String, String>();

    public String apply(StringBuilder sb) {
        int offset = 0;
        BufferMarker marker;
        SectionHeading sectionHeading;
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
        log.debug(Integer.toString(markerOffsets.size())
                + " markings: " + markerOffsets);
        if (markerOffsets.size() != sortedMarkers.size())
            throw new IllegalStateException(
                    "Markings/markers mismatch.  " + markerOffsets.size()
                    + " markings found, but there are " + size()
                    + " markers");
        // Can not run insert() until after the markers have been sorted.
        if (size() > 0) {
            for (BufferMarker m : values())
                if (m instanceof HeadingMarker) {
                    sectionHeading = ((HeadingMarker) m).getSectionHeading();
                    idToTextMap.put(sectionHeading.getXmlId(),
                            sectionHeading.getText());
                }
            validateAndSetClasses(sortedMarkers);
            log.debug(Integer.toString(sectionHeadings.size())
                    + " Section headings: " + sectionHeadings);
            // The list of markers MUST BE REVERSE SORTED before applying.
            // Applying in forward order would change buffer offsets.
            Collections.reverse(sortedMarkers);
            StringBuilder markerReport = new StringBuilder();
            for (BufferMarker m : sortedMarkers) {
                if (markerReport.length() > 0) markerReport.append(", ");
                markerReport.append(m.getIdString()
                        + '@' + m.getOffset());
                // N.b. this is where the real APPLY occurs to the buffer:
                m.updateBuffer();
            }
            log.debug("MARKERS:  " + markerReport.toString());
        }
        return sb.toString();
    }

    /**
     * Validates tag nesting and updates CSS classes of all TagMarkers.
     * Also populates the ordered sectionHeadings list.
     *
     * For efficiency of the iteration, these two disparate functions are both
     * performed by this one function.
     */
    private void validateAndSetClasses(List<BufferMarker> sortedMarkers) {
        sectionHeadings = new ArrayList<SectionHeading>();
        final List<TagMarker> stack = new ArrayList<TagMarker>();
        List<? extends TagMarker> typedStack = null;
        final List<String> queuedJcxClassNames = new ArrayList<String>();
        final List<String> queuedBlockClassNames = new ArrayList<String>();
        final List<String> queuedInlineClassNames = new ArrayList<String>();
        List<String> typedQueue = null;
        String linkText;
        CloseMarker closeM;
        LinkMarker lMarker;
        TagMarker lastTag, tagM;
        final List<JcxSpanMarker> jcxStack = new ArrayList<JcxSpanMarker>();
        final List<BlockMarker> blockStack = new ArrayList<BlockMarker>();
        final List<InlineMarker> inlineStack = new ArrayList<InlineMarker>();
        JcxSpanMarker prevJcx = null;
        BlockMarker prevBlock = null;
        InlineMarker prevInline = null;
        int headingLevel = 0;
        // We won't use slot 0 of nextSequence, so we can code using
        // headingLevel as the index.
        int[] nextSequence = new int[7];
        for (BufferMarker m : sortedMarkers) {
            if (m instanceof TagMarker) {
                tagM = (TagMarker) m;
                // Get this validation over with so rest of this block can
                // assume tagM is an instance of one of these types.
                if (!(tagM instanceof JcxSpanMarker)
                        && !(tagM instanceof BlockMarker)
                        && !(tagM instanceof InlineMarker))
                    throw new RuntimeException(
                            "Unexpected class for TagMarker " + tagM
                            + ": " + tagM.getClass().getName());
                // UPDATE prev/cur
                if (tagM.isAtomic()) {
                    // For atomics we do not deal with stacks, since we would
                    // just push and pop immediately resulting in no change.
                    // Similarly, whatever was cur* before will again be cur*
                    // when we exit this code block.
                    if (tagM instanceof JcxSpanMarker) {
                        prevJcx = (JcxSpanMarker) tagM;
                    } else if (tagM instanceof BlockMarker) {
                        prevBlock = (BlockMarker) tagM;
                    } else if (tagM instanceof InlineMarker) {
                        prevInline = (InlineMarker) tagM;
                    }
                } else {
                    // Tag has just OPENed.
                    // Opening a tag should not effect prev* tags, since nothing
                    // has closed to become previous.
                    if (tagM instanceof JcxSpanMarker) {
                        jcxStack.add((JcxSpanMarker) tagM);
                    } else if (tagM instanceof BlockMarker) {
                        blockStack.add((BlockMarker) tagM);
                    } else if (tagM instanceof InlineMarker) {
                        inlineStack.add((InlineMarker) tagM);
                    }
                    stack.add(tagM);  // 'lastTag' until another added
                }
                typedQueue = null;
                if (tagM instanceof JcxSpanMarker) {
                    if (queuedJcxClassNames.size() > 0)
                        typedQueue = queuedJcxClassNames;
                } else if (tagM instanceof BlockMarker) {
                    if (queuedBlockClassNames.size() > 0)
                        typedQueue = queuedBlockClassNames;
                } else if (tagM instanceof InlineMarker) {
                    if (queuedInlineClassNames.size() > 0)
                        typedQueue = queuedInlineClassNames;
                }
                if (typedQueue != null) {
                    for (String className : typedQueue) tagM.add(className);
                    typedQueue.clear();
                }
            } else if (m instanceof CloseMarker) {
                closeM = (CloseMarker) m;
                lastTag = (stack.size() > 0) ? stack.get(stack.size()-1) : null;
                // Validate tag name
                if (lastTag == null
                        || (lastTag.getTagName() == null
                            && closeM.getTagName() != null)
                        || (lastTag.getTagName() != null
                            && closeM.getTagName() == null)
                        || (lastTag.getTagName() != null
                        && !lastTag.getTagName().equals(closeM.getTagName())))
                    throw new CreoleParseException(
                            "Tangled tag nesting.  No matching open tag name "
                            + "for close of " + closeM + ".  Last open tag is "
                            + lastTag + '.');
                Boolean blockType = closeM.getBlockType();
                // Validate tag type
                if ((blockType == null && !(lastTag instanceof JcxSpanMarker))
                        || (blockType == Boolean.TRUE
                        && !(lastTag instanceof BlockMarker))
                        || (blockType == Boolean.FALSE
                        && !(lastTag instanceof InlineMarker)))
                    throw new CreoleParseException(
                            "Tangled tag nesting.  No matching open tag type "
                            + "for close of " + closeM + ".  Last open tag is "
                            + lastTag + '.');
                if (lastTag.isAtomic())
                    throw new CreoleParseException(
                            "Close tag " + closeM
                            + " attempted to close atomic tag "
                            + lastTag + '.');
                // Get this validation over with so rest of this block can
                // assume lastTag is an instance of one of these types.
                if (!(lastTag instanceof JcxSpanMarker)
                        && !(lastTag instanceof BlockMarker)
                        && !(lastTag instanceof InlineMarker))
                    throw new RuntimeException(
                            "Unexpected class for TagMarker " + lastTag
                            + ": " + lastTag.getClass().getName());
                // At this point we have validated match with an opening tag.
                if (lastTag instanceof JcxSpanMarker) {
                    prevJcx = (JcxSpanMarker) lastTag;
                    typedStack = jcxStack;
                } else if (lastTag instanceof BlockMarker) {
                    prevBlock = (BlockMarker) lastTag;
                    typedStack = blockStack;
                } else if (lastTag instanceof InlineMarker) {
                    prevInline = (InlineMarker) lastTag;
                    typedStack = inlineStack;
                }
                if (typedStack.size() < 1
                        || typedStack.get(typedStack.size()-1) != lastTag)
                    throw new CreoleParseException(
                            "Closing tag " + lastTag
                            + ", but it is not on the tail of the "
                            + "type-specific tag stack: " + typedStack);
                typedStack.remove(typedStack.size()-1);
                stack.remove(stack.size()-1);
            } else if (m instanceof Styler) {
                Styler styler = (Styler) m;
                TagType targetType = styler.getTargetType();
                String className = styler.getClassName();
                // Get this validation over with so rest of this block can
                // assume targetType is an instance of one of these types.
                switch (targetType) {
                  case INLINE:
                  case BLOCK:
                  case JCXSPAN:
                    break;
                  default:
                    throw new RuntimeException(
                            "Unexpected tag type value: " + targetType);
                }
                TagMarker targetTag = null;
                switch (styler.getTargetDirection()) {
                  case PREVIOUS:
                    switch (targetType) {
                      case INLINE:
                        targetTag = prevInline;
                        break;
                      case BLOCK:
                        targetTag = prevBlock;
                        break;
                      case JCXSPAN:
                        targetTag = prevJcx;
                        break;
                    }
                    if (targetTag == null)
                        throw new CreoleParseException(
                                "No previous " + targetType
                                + " tag for Styler " + styler);
                    break;
                  case CONTAINER:
                    switch (targetType) {
                      case INLINE:
                        typedStack = inlineStack;
                        break;
                      case BLOCK:
                        typedStack = blockStack;
                        break;
                      case JCXSPAN:
                        typedStack = jcxStack;
                        break;
                    }
                    if (typedStack.size() < 1)
                        throw new CreoleParseException(
                                "No parent " + targetType
                                + " container for Styler " + styler);
                    targetTag = typedStack.get(typedStack.size()-1);
                    break;
                  case NEXT:
                    switch (targetType) {
                      case INLINE:
                        typedQueue = queuedInlineClassNames;
                        break;
                      case BLOCK:
                        typedQueue = queuedBlockClassNames;
                        break;
                      case JCXSPAN:
                        typedQueue = queuedJcxClassNames;
                        break;
                    }
                    typedQueue.add(className);
                    break;
                  default:
                    throw new RuntimeException("Unexpected direction value: "
                            + styler.getTargetDirection());
                }
                if (targetTag != null) targetTag.add(className);
            } else if (m instanceof LinkMarker) {
                lMarker = (LinkMarker) m;
                linkText = lMarker.getLinkText();
                String lookedUpLabel =
                        idToTextMap.get(lMarker.getLinkText().substring(1));
                if (lMarker.getLabel() == null)
                    lMarker.setLabel((lookedUpLabel == null)
                            ? lMarker.getLinkText()
                            : lookedUpLabel);
                if (lookedUpLabel == null)
                    lMarker.wrapLabel("<span class=\"jcreole_orphanlink\">",
                            "</span>");
            } else {
                throw new CreoleParseException(
                        "Unexpected close marker class: "
                        + m.getClass().getName());
            }
            if (m instanceof HeadingMarker) {
                SectionHeading sh = ((HeadingMarker) m).getSectionHeading();
                sectionHeadings.add(sh);
                int newLevel = sh.getLevel();
                if (newLevel > headingLevel) {
                    headingLevel = newLevel;
                } else if (newLevel < headingLevel) {
                    for (int i = headingLevel; i >= newLevel; i--)
                        nextSequence[i] = 0;
                    headingLevel = newLevel;
                } else {
                    // No level change
                    // Intentionally empty
                }
                sh.setSequence(nextSequence[headingLevel]++);
            }
        }
        if (stack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched tag(s) generated: " + stack);
        if (jcxStack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched JCX tag(s): " + jcxStack);
        if (blockStack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched Block tag(s): " + blockStack);
        if (inlineStack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched Inline tag(s): " + inlineStack);
        if (queuedJcxClassNames.size() > 0)
            throw new CreoleParseException(
                    "Unapplied Styler JCX class names: " + queuedJcxClassNames);
        if (queuedBlockClassNames.size() > 0)
            throw new CreoleParseException(
                    "Unapplied Styler Block class names: "
                    + queuedBlockClassNames);
        if (queuedInlineClassNames.size() > 0)
            throw new CreoleParseException(
                    "Unapplied Styler Inline class names: "
                    + queuedInlineClassNames);
    }

    public List<SectionHeading> getSectionHeadings() {
        return sectionHeadings;
    }
}
