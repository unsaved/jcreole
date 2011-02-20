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

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.admc.jcreole.CreoleParseException;
import com.admc.jcreole.TagType;
import com.admc.jcreole.SectionHeading;
import com.admc.jcreole.Sections;
import com.admc.jcreole.EntryType;

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
    public StringBuilder apply(StringBuilder sb, String enumerationFormats,
            Map<String, Integer> footNoteNameMap,
            Map<String, Integer> glossaryNameMap) {
        if (enumerationFormats == null)
            throw new NullPointerException(
                    "enumerationFormats may not be null");
        buffer = sb;
        this.enumerationFormats = enumerationFormats;
        int nonBUMmarkerCount = unorderedPass(false);
        List<BufferMarker> sortedMarkers = new ArrayList(values());
        Collections.sort(sortedMarkers);
        // Can not run insert() until after the markers have been sorted.
        if (size() < 1) return buffer;
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
            if (!(m instanceof BodyUpdaterMarker)) m.updateBuffer();
        }
        log.debug("MARKERS:  " + markerReport.toString());

        // Can not move Entries until all of the normal \u001a markers have
        // been taken care of, because Styler directives depend on original
        // Creole sequence.

        // Extract all Entries
        int id;
        int offset3;
        int offset2 = -1;
        EntryType eType;
        String idString;
        Map<Integer, String> idToGloss = new HashMap<Integer, String>();
        Map<Integer, String> idToFoot = new HashMap<Integer, String>();
        while ((offset2 = buffer.indexOf("\u0002", offset2 + 1)) > -1) {
            if (buffer.length() < offset2 + 6)
                throw new CreoleParseException(
                        "\\u0002 Marking too close to end of output");
            // Unfortunately StringBuilder has no indexOf(char).
            // We could do StringBuilder.toString().indexOf(char), but
            // that's a pretty expensive copy operation.
            offset3 = buffer.indexOf("\u0003", offset2 + 6);
            if (offset3 < 0)
                throw new CreoleParseException("No termination for Entry");
            switch (buffer.charAt(offset2 + 1)) {
              case 'F':
                  eType = EntryType.FOOTNOTE;
                  break;
              case 'G':
                  eType = EntryType.GLOSSARY;
                  break;
              default:
                  throw new CreoleParseException(
                        "Unexpected EntryType indicator: "
                        + buffer.charAt(offset2 + 1));
            }
            idString = buffer.substring(offset2 + 2, offset2 + 6);
            id = Integer.parseInt(idString, 16);
            if (!((eType == EntryType.FOOTNOTE)
                    ? footNoteNameMap : glossaryNameMap)
                    .containsValue(Integer.valueOf(id)))
                throw new CreoleParseException("Missing "
                    + ((eType == EntryType.FOOTNOTE) ? "footNote" : "glossary")
                    + " entry w/ id " + id);
            ((eType == EntryType.FOOTNOTE) ? idToFoot : idToGloss)
                    .put(Integer.valueOf(id),
                    buffer.substring(offset2 + 6, offset3));
            buffer.delete(offset2, offset3 +1);
        }
        if (glossaryNameMap.size() != idToGloss.size())
            throw new IllegalStateException("Glossary entry mismatch.  "
                    + glossaryNameMap.size() + ' ' + " names parsed, but "
                    + idToGloss.size() + " entries marked");
        if (footNoteNameMap.size() != idToFoot.size())
            throw new IllegalStateException("Footnote entry mismatch.  "
                    + footNoteNameMap.size() + ' ' + " names parsed, but "
                    + idToFoot.size() + " entries marked");
        for (Map.Entry<String, Integer> e : glossaryNameMap.entrySet()) {
            if (!idToGloss.containsKey(e.getValue()))
                throw new IllegalStateException("Glossary Entry for name "
                        + e.getKey() + " is missing");
            nameToHtml.put(e.getKey(), idToGloss.get(e.getValue()));
        }

        // TODO: Consider whether to check for \u001a's inside of Entry p's,
        // which must be circular Glossary or FootNotes markers.
        int bUMmarkerCount = unorderedPass(true);
        if (nonBUMmarkerCount + bUMmarkerCount != sortedMarkers.size())
            throw new IllegalStateException(
                    "Markings/markers mismatch.  " + nonBUMmarkerCount
                    + " + " + bUMmarkerCount
                    + " markings found, but there are " + size()
                    + " markers");
        Collections.sort(sortedMarkers);
        Set<String> mappedNames = new HashSet<String>();
        FootNoteRefMarker fnrm;
        StringBuilder footNotesBuffer = new StringBuilder();
        for (BufferMarker m : sortedMarkers)
            if (m instanceof FootNoteRefMarker) {
                fnrm = (FootNoteRefMarker) m;
                if (mappedNames.contains(fnrm.getName())) continue;
                mappedNames.add(fnrm.getName());
                if (!idToFoot.containsKey(fnrm.getTargNum()))
                    throw new CreoleParseException(
                            "Orphan footnote ref: " + fnrm.getName());
                footNotesBuffer.append("<dl>\n  <dt>")
                        .append(fnrm.getRefNum()).append("</td>\n  <dd>")
                        .append(idToFoot.remove(fnrm.getTargNum()))
                        .append("</dd>\n</dl>\n");
            }
        for (Map.Entry<Integer, String> e : idToFoot.entrySet()) {
            log.warn("Unreferenced footnote with HTML id " + e.getKey());
            footNotesBuffer.append("<dl>\n  <dt>")
                    .append("unreferenced").append("</td>\n  <dd>")
                    .append(e.getValue()).append("</dd>\n</dl>\n");
        }
        Collections.reverse(sortedMarkers);
        StringBuilder glossaryBuffer = new StringBuilder();
        for (Map.Entry<String, String> e :
                new TreeMap<String, String>(nameToHtml).entrySet())
            glossaryBuffer.append("<dl>\n  <dt>")
                    .append(e.getKey()).append("</td>\n  <dd>")
                    .append(e.getValue()).append("</dd>\n</dl>\n");
        for (BufferMarker m : sortedMarkers) {
            if (m instanceof GlossaryMarker) {
                ((GlossaryMarker) m).setBody(glossaryBuffer.toString());
                m.updateBuffer();
            } else if (m instanceof FootNotesMarker) {
                ((FootNotesMarker) m).setBody(footNotesBuffer.toString());
                m.updateBuffer();
            }
        }
        return buffer;
    }

    private Map<String, String> nameToHtml = new HashMap<String, String>();

    /**
     * Sets context (buffer and offset) for markers.
     *
     * @param bodyUpdaterMarkers If true then only update BodyUpdaterMakers,
     *        otherwise then only update non-BodyUpdaterMarkers.
     * @return Number of markers found
     */
    private int unorderedPass(boolean bodyUpdaterMarkers) {
        BufferMarker marker;
        List<Integer> markerOffsets = new ArrayList<Integer>();
        String idString;
        int id;
        int offset = 0;
        while ((offset = buffer.indexOf("\u001a", offset)) > -1) try {
            // Unfortunately StringBuilder has no indexOf(char).
            // We could do StringBuilder.toString().indexOf(char), but
            // that's a pretty expensive copy operation.
            if (buffer.length() < offset + 4)
                throw new CreoleParseException(
                        "Marking too close to end of output");
            idString = buffer.substring(offset + 1, offset + 5);
            id = Integer.parseInt(idString, 16);
            marker = get(Integer.valueOf(id));
            if (marker == null)
                throw new IllegalStateException("Lost marker with id " + id);
            if (bodyUpdaterMarkers) {
                if (!(marker instanceof BodyUpdaterMarker)) continue;
            } else {
                if (marker instanceof BodyUpdaterMarker) continue;
            }
            markerOffsets.add(Integer.valueOf(offset));
            marker.setContext(buffer, offset);
        } finally {
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
        Map<String, Integer> nameToRefNum = new HashMap<String, Integer>();
        int refNum = 0;
        FootNoteRefMarker fnrm;
        for (BufferMarker m : values())
            if (m instanceof HeadingMarker) {
                hm = (HeadingMarker) m;
                sectionHeading = hm.getSectionHeading();
                idToTextMap.put(sectionHeading.getXmlId(),
                        sectionHeading.getText());
            } else if (m instanceof TocMarker) {
                ((TocMarker) m).setDefaultLevelInclusions(enumerationFormats);
            } else if (m instanceof FootNoteRefMarker) {
                fnrm = (FootNoteRefMarker) m;
                if (nameToRefNum.containsKey(fnrm.getName())) {
                    fnrm.setRefNum(nameToRefNum.get(fnrm.getName()));
                } else {
                    fnrm.setRefNum(++refNum);
                    nameToRefNum.put(fnrm.getName(), refNum);
                }
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
                    linkM.wrapLabel(
                            "<span class=\"jcreole_orphanLink\">", "</span>");
            } else if (m instanceof TocMarker) {
                ((TocMarker) m).setSectionHeadings(sections);
            } else if (m instanceof BodyUpdaterMarker) {
                ;
            } else if (m instanceof FootNoteRefMarker) {
                ;
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
