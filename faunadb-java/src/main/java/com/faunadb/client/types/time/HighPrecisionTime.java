package com.faunadb.client.types.time;

import org.joda.time.Instant;
import org.joda.time.format.ISODateTimeFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

/**
 * Represents an instant in time with nanosecond precision.
 */
public class HighPrecisionTime implements Comparable<HighPrecisionTime> {

  private static final int NANOS_IN_A_MIRO = 1000;
  private static final int NANOS_IN_A_MILLI = 1000000;
  private static final int NANOS_IN_A_SECOND = 1000000000;
  private static final int MILLIS_IN_A_SECOND = 1000;

  private static final Pattern PRECISION_GROUPS = Pattern.compile("\\.\\d{3}(?<micros>\\d{3})(?<nanos>\\d{3})?Z");

  /**
   * Parses a string into a {@link HighPrecisionTime} instance.
   *
   * @param value string formated as yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ
   * @return an new instance of {@link HighPrecisionTime}
   */
  public static HighPrecisionTime parse(String value) {
    Instant initialTime = ISODateTimeFormat.dateTimeParser().parseDateTime(value).toInstant();
    Matcher precision = PRECISION_GROUPS.matcher(value);

    if (precision.find()) {
      return create(
        initialTime,
        toLong(precision.group("micros")),
        toLong(precision.group("nanos"))
      );
    }

    return create(initialTime, 0, 0);
  }

  private static long toLong(String value) {
    return value != null ? Long.valueOf(value) : 0;
  }

  /**
   * Creates a new instance of {@link HighPrecisionTime} from a {@link Instant}.
   *
   * @param initialTime initial timestamp
   */
  public static HighPrecisionTime fromInstant(Instant initialTime) {
    return create(initialTime, 0, 0);
  }

  /**
   * Creates a new instance of {@link HighPrecisionTime}. Nanoseconds overflow will
   * be calculated and added to the initial timestamp.
   * <p>
   * For example:
   * <pre>
   * {@code HighPrecisionTime.withNanos(new Instant(1), 0) == HighPrecisionTime.withNanos(new Instant(0), 1000000)}
   * </pre>
   *
   * @param initialTime initial timestamp
   * @param nanosToAdd  nanoseconds to add to the initial timestamp
   */
  public static HighPrecisionTime fromInstantWithNanos(Instant initialTime, long nanosToAdd) {
    return create(initialTime, 0, nanosToAdd);
  }

  /**
   * Creates a new instance of {@link HighPrecisionTime}. Microseconds overflows will
   * be calculated and added to the initial timestamp.
   * <p>
   * For example:
   * <pre>
   * {@code HighPrecisionTime.fromInstant(new Instant(1), 0) == HighPrecisionTime.fromInstant(new Instant(0), 1000)}
   * </pre>
   *
   * @param initialTime initial timestamp
   * @param microsToAdd  nanoseconds to add to the initial timestamp
   */
  public static HighPrecisionTime fromInstantWithMicros(Instant initialTime, long microsToAdd) {
    return create(initialTime, microsToAdd, 0);
  }

  private static HighPrecisionTime create(Instant initialTime, long microsToAdd, long nanosToAdd) {
    requireNonNull(initialTime);

    long nanos =
      initialTime.getMillis() % MILLIS_IN_A_SECOND * NANOS_IN_A_MILLI +
        microsToAdd * NANOS_IN_A_MIRO +
        nanosToAdd;

    return new HighPrecisionTime(initialTime.getMillis() / MILLIS_IN_A_SECOND, nanos);
  }

  private final long secondsSinceEpoch;
  private final int nanoSecondsOffset;

  /**
   * Creates a new instance of {@link HighPrecisionTime}. Nano to seconds overflow will be calculated.
   *
   * @param secondsSinceEpoch seconds passed since Java's epoch
   * @param nanoSecondsOffset nanoseconds offset
   */
  public HighPrecisionTime(long secondsSinceEpoch, long nanoSecondsOffset) {
    this.secondsSinceEpoch = secondsSinceEpoch + nanoSecondsOffset / NANOS_IN_A_SECOND;
    this.nanoSecondsOffset = (int) (nanoSecondsOffset % NANOS_IN_A_SECOND);
  }

  /**
   * Returns an instance of {@link Instant}.
   * Truncates nanoseconds.
   *
   * @return trucated timestamp
   */
  public Instant toInstant() {
    return new Instant(getMillisecondsFromEpoch());
  }

  /**
   * Returns the number of milliseconds since Java's Eposh.
   * Truncates nanoseconds.
   *
   * @return number of milliseconds
   * @see HighPrecisionTime#HighPrecisionTime(long, long)
   */
  public long getMillisecondsFromEpoch() {
    return secondsSinceEpoch * MILLIS_IN_A_SECOND +
      nanoSecondsOffset / NANOS_IN_A_MILLI;
  }

  /**
   * Returns the number of nanosecond adjustment to the initial timestamp.
   *
   * @return number of nanoseconds
   * @see HighPrecisionTime#HighPrecisionTime(long, long)
   */
  public long getNanosecondsOffset() {
    return nanoSecondsOffset;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HighPrecisionTime))
      return false;

    HighPrecisionTime other = (HighPrecisionTime) obj;
    return this.secondsSinceEpoch == other.secondsSinceEpoch
      && this.nanoSecondsOffset == other.nanoSecondsOffset;
  }

  @Override
  public int hashCode() {
    return hash(secondsSinceEpoch, nanoSecondsOffset);
  }

  @Override
  public int compareTo(HighPrecisionTime other) {
    int compareSeconds = Long.compare(secondsSinceEpoch, other.secondsSinceEpoch);
    if (compareSeconds != 0) return compareSeconds;

    return Integer.compare(nanoSecondsOffset, other.nanoSecondsOffset);
  }

  @Override
  public String toString() {
    return format(
      "%s.%09dZ",
      toInstant().toString(ISODateTimeFormat.dateHourMinuteSecond()),
      nanoSecondsOffset
    );
  }
}
