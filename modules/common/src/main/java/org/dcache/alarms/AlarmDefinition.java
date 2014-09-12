/*
COPYRIGHT STATUS:
Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
software are sponsored by the U.S. Department of Energy under Contract No.
DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
non-exclusive, royalty-free license to publish or reproduce these documents
and software for U.S. Government purposes.  All documents and software
available from this server are protected under the U.S. and Foreign
Copyright Laws, and FNAL reserves all rights.

Distribution of the software available from this server is free of
charge subject to the user following the terms of the Fermitools
Software Legal Information.

Redistribution and/or modification of the software shall be accompanied
by the Fermitools Software Legal Information  (including the copyright
notice).

The user is asked to feed back problems, benefits, and/or suggestions
about the software to the Fermilab Software Providers.

Neither the name of Fermilab, the  URA, nor the names of the contributors
may be used to endorse or promote products derived from this software
without specific prior written permission.

DISCLAIMER OF LIABILITY (BSD):

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.

Liabilities of the Government:

This software is provided by URA, independent from its Prime Contract
with the U.S. Department of Energy. URA is acting independently from
the Government and in its own private capacity and is not acting on
behalf of the U.S. Government, nor as its contractor nor its agent.
Correspondingly, it is understood and agreed that the U.S. Government
has no connection to this software and in no manner whatsoever shall
be liable for nor assume any responsibility or obligation for any claim,
cost, or damages arising out of or resulting from the use of the software
available from this server.

Export Control:

All documents and software available from this server are subject to U.S.
export control laws.  Anyone downloading information from this server is
obligated to secure any necessary Government licenses before exporting
documents or software obtained from this server.
 */
package org.dcache.alarms;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.regex.Pattern;

import org.dcache.util.RegexUtils;

/**
 * Provides the structure for an external, custom definition of an alarm.<p>
 *
 * <table>
 * <tr>
 * <td>PROPERTY</td>
 * <td>REQUIRED</td>
 * <td>POSSIBLE VALUES</td>
 * </tr>
 *  <tr>
 * <td>type</td>
 * <td>YES</td>
 * <td>name of this alarm type (settable only once).</td>
 * </tr>
 * <tr>
 * <td>keyWords</td>
 * <td>YES</td>
 * <td>whitespace-delimited concatenation of key field names (see below).</td>
 * </tr>
 * <tr>
 * <td>regex</td>
 * <td>YES</td>
 * <td>a pattern to match the message with; note: it is advisable to place this
 * pattern in double quotes: e.g., "[=].+[\w]*".</td>
 * </tr>
 * <tr>
 * <td>regexFlags</td>
 * <td>NO</td>
 * <td>options for regex (these are string representations of the
 * {@link Pattern} options, joined by the 'or' pipe symbol: e.g.,
 * "CASE_INSENSITIVE | DOTALL").</td>
 * </tr>
 * <tr>
 * <td>matchException</td>
 * <td>NO</td>
 * <td>true = recur over embedded exception messages when applying regex match
 * (default is false).</td>
 * </tr>
 * <tr>
 * <td>depth</td>
 * <td>NO</td>
 * <td>depth of exception trace to examine when applying match-exception;
 * undefined means unbounded (default).</td>
 * </tr>
 * </table>
 *
 * <p>
 * An example:
 * </p>
 *
 * <pre>
 *       &lt;alarmType&gt;
 *          &lt;type&gt;SERVICE_CREATION_FAILURE&lt;/type&gt;
 *          &lt;regex&gt;(.+) from ac_create&lt;/regex&gt;
 *          &lt;keyWords&gt;group1 type host domain service&lt;/keyWords&gt;
 *       &lt;/alarmType&gt;
 * </pre>
 *
 * <p>
 * The field names which can be used to constitute the unique key of the alarm
 * include the properties defined for all alarms,
 * plus the parsing of the message field into regex groups:
 * </p>
 *
 * <ol>
 * <li>timestamp</li>
 * <li>domain</li>
 * <li>service</li>
 * <li>host</li>
 * <li>message (= group0)</li>
 * <li>group + number</li>
 * </ol>
 *
 * <p>
 * Alarms that are generated by the code at the origin of the problem
 * may also add other arbitrary key word identifiers, but custom
 * definitions are limited to the values associated with these fixed
 * fields.
 * </p>
 *
 * @author arossi
 */
public interface AlarmDefinition extends Alarm {
    String ALARM_TAG = "alarm";
    String DEPTH_TAG = "depth";
    String GROUP_TAG = "group";
    int GROUP_TAG_LENGTH = GROUP_TAG.length();
    String KEY_WORDS_TAG = "keyWords";
    String MATCH_EXCEPTION_TAG = "matchException";
    String MESSAGE_TAG = "message";
    String REGEX_TAG = "regex";
    String REGEX_FLAGS_TAG = "regexFlags";
    String KEY_WORD_DELIMITER = "[\\s]";
    String TIMESTAMP_TAG = "timestamp";
    String TYPE_TAG = "type";
    String RM = "-";
    String REQUIRED = " is a required attribute.";

    ImmutableList<String> ATTRIBUTES
        = new ImmutableList.Builder<String>()
        .add(TYPE_TAG)
        .add(KEY_WORDS_TAG)
        .add(REGEX_TAG)
        .add(REGEX_FLAGS_TAG)
        .add(MATCH_EXCEPTION_TAG)
        .add(DEPTH_TAG)
        .build();

    ImmutableSet<String> KEY_VALUES
        = new ImmutableSet.Builder<String>()
        .add(TIMESTAMP_TAG)
        .add(TYPE_TAG)
        .add(Alarm.DOMAIN_TAG)
        .add(Alarm.SERVICE_TAG)
        .add(Alarm.HOST_TAG)
        .add(MESSAGE_TAG)
        .add(GROUP_TAG + "N")
        .build();

    ImmutableList<String> DEFINITIONS
        = new ImmutableList.Builder<String>()
        .add("Choose a name to call this type of alarm (required).")
        .add("Create the unique identifier for this alarm event based on"
                    + " the selected fields (whitespace delimited) "
                    + KEY_VALUES
                    + " (required).")
        .add("Java-style regular expression used to match messages "
                    + " (required).")
        .add("Java-style flag options for regex; join using '|' (or) "
                    + RegexUtils.FLAG_VALUES
                    + " (optional; default: none).")
        .add("Apply the regex to nested exception messages "
                    + " (boolean, optional; default: false).")
        .add("Match nested exception messages using regex only to this level"
                    + " (integer, optional; default: undefined).")
        .build();

    /**
     * @return depth to which the matcher should attempt to find a match in
     *         the embedded exception stack. -1 means unlimited (to root
     *         cause).
     */
    Integer getDepth();

    /**
     * Generate the key from the passed in values according to the
     * key word names that have been set.
     *
     * @param formattedMessage of the logging event
     * @param timestamp of the logging event
     * @param host from diagnostic context
     * @param domain from diagnostic context
     * @param service from diagnostic context
     * @return string uniquely identifying this alert.
     */
    String createKey(String formattedMessage,
                  long timestamp,
                  String host,
                  String domain,
                  String service);

    /**
     * @return whitespace-delimited set of key words whose values form
     *         the unique identifier for this alert.
     */
    String getKeyWords();

    /**
     * @return if true, use the regular expression to try to match against
     *         embedded exception stack.  Default is false (match main message
     *         only).
     */
    Boolean isMatchException();

    /**
     * @return the optional Java regular expression flags used to compile the
     *         pattern.
     */
    String getRegexFlags();

    /**
     * @return the compiled regular expression to use to match logging events
     *         against this definition.
     */
    Pattern getRegexPattern();

    /**
     * @return the raw string regular expression to use to match logging events
     *         against this definition.
     */
    String getRegexString();

    /**
     * @param depth to which the matcher should attempt to find a match in
     *         the embedded exception stack. -1 means unlimited (to root
     *         cause).
     */
    void setDepth(Integer depth);

    /**
     * @param keyWords whitespace-delimited set of key words whose values form
     *         the unique identifier for this alert.
     */
    void setKeyWords(String keyWords);

    /**
     * @param matchException use the regular expression to try to match against
     *         embedded exception stack.  Default is false (match main message
     *         only).
     */
    void setMatchException(Boolean matchException);

    /**
     * @param regexString the raw string regular expression to use to match logging events
     *         against this definition.
     */
    void setRegexString(String regexString);

    /**
     * @param regexFlags the optional Java regular expression flags used to compile the
     *         pattern.
     */
    void setRegexFlags(String regexFlags);

    /**
     * @param type name of this alarm type.
     */
    void setType(String type);

    /**
     * Check for the validity of the definition (whether required fields are
     * set, etc.).
     *
     * @throws AlarmDefinitionValidationException
     */
    void validate() throws AlarmDefinitionValidationException;

    /**
     * Set a field property and make sure the value is valid.
     *
     * @throws AlarmDefinitionValidationException
     */
    void validateAndSet(String name,
                        String value) throws AlarmDefinitionValidationException;
}