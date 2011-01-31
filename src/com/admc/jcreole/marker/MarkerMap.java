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

import java.util.Arrays;
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
import com.admc.jcreole.Sections;

public class MarkerMap extends HashMap<Integer, BufferMarker> {
    private static Log log = LogFactory.getLog(MarkerMap.class);
    private Sections sections;
    private Map<String, String> idToTextMap = new HashMap<String, String>();
    private String enumerationFormats;
    private StringBuilder buffer;

    /**
     * @param enumerationFormats is the starting numerationFormats used for
     *        header elements in the main body (the current body
     *        enumerationFormats can change as HeadingMarkers are encountered).
     *        This setting is independent of TOC levelInclusions, which is
     *        encapsulated nicely in TocMarker instances and not changed here
     *        (or elsewhere).
     */
    public String apply(StringBuilder sb, String enumerationFormats) {
        if (enumerationFormats == null)
            throw new NullPointerException(
                    "enumerationFormats may not be null");
        buffer = sb;
        this.enumerationFormats = enumerationFormats;
        int markerCount = unorderedPass();
        List<BufferMarker> sortedMarkers = new ArrayList(values());
        Collections.sort(sortedMarkers);
        if (markerCount != sortedMarkers.size())
            throw new IllegalStateException(
                    "Markings/markers mismatch.  " + markerCount
                    + " markings found, but there are " + size()
                    + " markers");
        // Can not run insert() until after the markers have been sorted.
        if (size() > 0) {
            forwardPass1(sortedMarkers);
            forwardPass2(sortedMarkers);
            log.debug(Integer.toString(sections.size())
                    + " Section headings: " + sections);
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
        return buffer.toString();
    }

    /**
     * @return Number of markers found
     */
    private int unorderedPass() {
        BufferMarker marker;
        List<Integer> markerOffsets = new ArrayList<Integer>();
        String idString;
        int id;
        int offset = 0;
        while ((offset = buffer.indexOf("\u001a", offset)) > -1) {
            // Unfortunately StringBuilder has no indexOf(char).
            // We could make do StringBuilder.toString().indexOf(char), but
            // that's a pretty expensive copy operation.
            markerOffsets.add(Integer.valueOf(offset));
            if (buffer.length() < offset + 4)
                throw new CreoleParseException(
                        "Marking too close to end of output");
            idString = buffer.substring(offset + 1, offset + 5);
            id = Integer.parseInt(idString, 16);
            marker = get(Integer.valueOf(id));
            if (marker == null)
                throw new IllegalStateException("Lost marker with id " + id);
            marker.setContext(buffer, offset);
            offset += 5;  // Move past the marker that we just found
        }
        log.debug(Integer.toString(markerOffsets.size())
                + " markings: " + markerOffsets);
        return markerOffsets.size();
    }

    /**
     * Marker prep which must occur completely before forwardPass2
     */
    private void forwardPass1(List<BufferMarker> sortedMarkers) {
        HeadingMarker hm;
        SectionHeading sectionHeading;
        for (BufferMarker m : values())
            if (m instanceof HeadingMarker) {
                hm = (HeadingMarker) m;
                sectionHeading = hm.getSectionHeading();
                idToTextMap.put(sectionHeading.getXmlId(),
                        sectionHeading.getText());
            } else if (m instanceof TocMarker) {
                ((TocMarker) m).setDefaultLevelInclusions(enumerationFormats);
            }
    }

    /**
     * Does lots of stuff during a strictly forward-direction iteration of
     * all Markers .
     * <p>
     * In particular, any automatic behavior dependent upon position within the
     * document must be implemented here.
     * </p>
     */
    private void forwardPass2(List<BufferMarker> sortedMarkers) {
        sections = new Sections();
        final List<TagMarker> stack = new ArrayList<TagMarker>();
        List<? extends TagMarker> typedStack = null;
        final List<String> queuedJcxSpanClassNames = new ArrayList<String>();
        final List<String> queuedJcxBlockClassNames = new ArrayList<String>();
        final List<String> queuedBlockClassNames = new ArrayList<String>();
        final List<String> queuedInlineClassNames = new ArrayList<String>();
        List<String> typedQueue = null;
        String linkText;
        CloseMarker closeM;
        LinkMarker linkM;
        HeadingMarker headingM;
        TagMarker lastTag, tagM;
        final List<JcxSpanMarker> jcxSpanStack = new ArrayList<JcxSpanMarker>();
        final List<JcxBlockMarker> jcxBlockStack =
                new ArrayList<JcxBlockMarker>();
        final List<BlockMarker> blockStack = new ArrayList<BlockMarker>();
        final List<InlineMarker> inlineStack = new ArrayList<InlineMarker>();
        JcxSpanMarker prevJcxSpan = null;
        JcxBlockMarker prevJcxBlock = null;
        BlockMarker prevBlock = null;
        InlineMarker prevInline = null;
        int headingLevel = 0;
        int[] curSequences = new int[] {-1, -1, -1, -1, -1, -1};
        for (BufferMarker m : sortedMarkers) {
            if (m instanceof TagMarker) {
                tagM = (TagMarker) m;
                // Get this validation over with so rest of this block can
                // assume tagM is an instance of one of these types.
                if (!(tagM instanceof JcxSpanMarker)
                        && !(tagM instanceof JcxBlockMarker)
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
                        prevJcxSpan = (JcxSpanMarker) tagM;
                    } else if (tagM instanceof JcxBlockMarker) {
                        prevJcxBlock = (JcxBlockMarker) tagM;
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
                        jcxSpanStack.add(0, (JcxSpanMarker) tagM);
                    } else if (tagM instanceof JcxBlockMarker) {
                        jcxBlockStack.add(0, (JcxBlockMarker) tagM);
                    } else if (tagM instanceof BlockMarker) {
                        blockStack.add(0, (BlockMarker) tagM);
                    } else if (tagM instanceof InlineMarker) {
                        inlineStack.add(0, (InlineMarker) tagM);
                    }
                    stack.add(0, tagM);  // 'lastTag' until another added
                }
                typedQueue = null;
                if (tagM instanceof JcxSpanMarker) {
                    if (queuedJcxSpanClassNames.size() > 0)
                        typedQueue = queuedJcxSpanClassNames;
                } else if (tagM instanceof JcxBlockMarker) {
                    if (queuedJcxBlockClassNames.size() > 0)
                        typedQueue = queuedBlockClassNames;
                } else if (tagM instanceof BlockMarker) {
                    if (queuedBlockClassNames.size() > 0)
                        typedQueue = queuedBlockClassNames;
                } else if (tagM instanceof InlineMarker) {
                    if (queuedInlineClassNames.size() > 0)
                        typedQueue = queuedInlineClassNames;
                }
                if (typedQueue != null) {
                    tagM.addCssClasses(typedQueue.toArray(new String[0]));
                    typedQueue.clear();
                }
            } else if (m instanceof CloseMarker) {
                closeM = (CloseMarker) m;
                lastTag = (stack.size() > 0) ? stack.get(0) : null;
                // Validate tag type
                TagType targetType= closeM.getTargetType();
                try { switch (targetType) {
                  case JCXBLOCK:
                    if (!(lastTag instanceof JcxBlockMarker))
                        throw new Exception();
                    break;
                  case JCXSPAN:
                    if (!(lastTag instanceof JcxSpanMarker))
                        throw new Exception();
                    break;
                  case BLOCK:
                    if (!(lastTag instanceof BlockMarker))
                        throw new Exception();
                    break;
                  case INLINE:
                    if (!(lastTag instanceof InlineMarker))
                        throw new Exception();
                    break;
                  default:
                    throw new IllegalStateException(
                            "Unexpected target tag type: " + targetType);
                } } catch (Exception e) {
                    throw new CreoleParseException(
                            "Tangled tag nesting.  No matching open "
                            + targetType + " tag for close of "
                            + closeM + ".  Last open tag is "
                            + lastTag + '.', e);
                }
                if (lastTag.isAtomic())
                    throw new CreoleParseException(
                            "Close tag " + closeM
                            + " attempted to close atomic tag "
                            + lastTag + '.');
                // Get this validation over with so rest of this block can
                // assume lastTag is an instance of one of these types.
                if (!(lastTag instanceof JcxSpanMarker)
                        && !(lastTag instanceof JcxBlockMarker)
                        && !(lastTag instanceof BlockMarker)
                        && !(lastTag instanceof InlineMarker))
                    throw new RuntimeException(
                            "Unexpected class for TagMarker " + lastTag
                            + ": " + lastTag.getClass().getName());
                // At this point we have validated match with an opening tag.
                if (lastTag instanceof JcxSpanMarker) {
                    prevJcxSpan = (JcxSpanMarker) lastTag;
                    typedStack = jcxSpanStack;
                } else if (lastTag instanceof JcxBlockMarker) {
                    prevJcxBlock = (JcxBlockMarker) lastTag;
                    typedStack = jcxBlockStack;
                } else if (lastTag instanceof BlockMarker) {
                    prevBlock = (BlockMarker) lastTag;
                    typedStack = blockStack;
                } else if (lastTag instanceof InlineMarker) {
                    prevInline = (InlineMarker) lastTag;
                    typedStack = inlineStack;
                }
                if (typedStack.size() < 1 || typedStack.get(0) != lastTag)
                    throw new CreoleParseException(
                            "Closing tag " + lastTag
                            + ", but it is not on the tail of the "
                            + "type-specific tag stack: " + typedStack);
                typedStack.remove(0);
                stack.remove(0);
            } else if (m instanceof Styler) {
                Styler styler = (Styler) m;
                TagType targetType = styler.getTargetType();
                String[] classNames = styler.getClassNames();
                // Get this validation over with so rest of this block can
                // assume targetType is an instance of one of these types.
                switch (targetType) {
                  case INLINE:
                  case BLOCK:
                  case JCXSPAN:
                  case JCXBLOCK:
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
                        targetTag = prevJcxSpan;
                        break;
                      case JCXBLOCK:
                        targetTag = prevJcxBlock;
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
                        typedStack = jcxSpanStack;
                        break;
                      case JCXBLOCK:
                        typedStack = jcxBlockStack;
                        break;
                    }
                    if (typedStack.size() < 1)
                        throw new CreoleParseException(
                                "No parent " + targetType
                                + " container for Styler " + styler);
                    targetTag = typedStack.get(0);
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
                        typedQueue = queuedJcxSpanClassNames;
                        break;
                      case JCXBLOCK:
                        typedQueue = queuedJcxBlockClassNames;
                        break;
                    }
                    typedQueue.addAll(Arrays.asList(classNames));
                    break;
                  default:
                    throw new RuntimeException("Unexpected direction value: "
                            + styler.getTargetDirection());
                }
                if (targetTag != null) targetTag.addCssClasses(classNames);
            } else if (m instanceof LinkMarker) {
                linkM = (LinkMarker) m;
                linkText = linkM.getLinkText();
                String lookedUpLabel =
                        idToTextMap.get(linkM.getLinkText().substring(1));
                if (linkM.getLabel() == null)
                    linkM.setLabel((lookedUpLabel == null)
                            ? linkM.getLinkText()
                            : lookedUpLabel);
                if (lookedUpLabel == null)
                    linkM.wrapLabel("<span class=\"jcreole_orphanlink\">",
                            "</span>");
            } else if (m instanceof TocMarker) {
                ((TocMarker) m).setSectionHeadings(sections);
            } else {
                throw new CreoleParseException(
                        "Unexpected close marker class: "
                        + m.getClass().getName());
            }
            if (m instanceof HeadingMarker) {
                headingM = (HeadingMarker) m;
                SectionHeading sh = headingM.getSectionHeading();
                enumerationFormats =
                        headingM.updatedEnumerationFormats(enumerationFormats);
                sh.setEnumerationFormats(enumerationFormats);
                sections.add(sh);
                int newLevel = sh.getLevel();
                if (newLevel > headingLevel) {
                    headingLevel = newLevel;
                } else if (newLevel < headingLevel) {
                    for (int i = headingLevel; i > newLevel; i--)
                        curSequences[i-1] = -1;
                    headingLevel = newLevel;
                } else {
                    // No level change
                    // Intentionally empty
                }
                if (headingM.getFormatReset() != null)
                    curSequences[headingLevel-1] = -1;
                curSequences[headingLevel-1] += 1;
                sh.setSequences(curSequences);
            }
        }
        if (stack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched tag(s) generated: " + stack);
        if (jcxSpanStack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched JCX Span tag(s): " + jcxSpanStack);
        if (blockStack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched Block tag(s): " + blockStack);
        if (jcxBlockStack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched JCX Block tag(s): " + jcxBlockStack);
        if (inlineStack.size() != 0)
            throw new CreoleParseException(
                    "Unmatched Inline tag(s): " + inlineStack);
        if (queuedJcxSpanClassNames.size() > 0)
            throw new CreoleParseException(
                    "Unapplied Styler JCX class names: "
                    + queuedJcxSpanClassNames);
        if (queuedJcxBlockClassNames.size() > 0)
            throw new CreoleParseException(
                    "Unapplied Styler JCX Block class names: "
                    + queuedBlockClassNames);
        if (queuedBlockClassNames.size() > 0)
            throw new CreoleParseException(
                    "Unapplied Styler Block class names: "
                    + queuedBlockClassNames);
        if (queuedInlineClassNames.size() > 0)
            throw new CreoleParseException(
                    "Unapplied Styler Inline class names: "
                    + queuedInlineClassNames);
    }

    public Sections getSectionHeadings() {
        return sections;
    }
}
