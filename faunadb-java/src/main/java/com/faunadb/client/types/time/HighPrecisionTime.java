package com.faunadb.client.types.time;

import org.joda.time.Instant;
import org.joda.time.format.ISODateTimeFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

/**
 * Wraps an instance of {@link Instant} and adds micro and nanosecond precision to it.
 */
public class HighPrecisionTime {

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
      return new HighPrecisionTime(
        initialTime,
        toInt(precision.group("micros")),
        toInt(precision.group("nanos"))
      );
    }

    return new HighPrecisionTime(initialTime, 0, 0);
  }

  private static int toInt(String value) {
    return value != null ? Integer.valueOf(value) : 0;
  }

  private final Instant truncated;
  private final int nanosToAdd;

  /**
   * Creates a new instance of {@link HighPrecisionTime}. Nano and microseconds overflows will
   * be calculated and added to the initial timestamp.
   * <p>
   * For example:
   * <ul>
   * <li>{@code new HighPrecisionTime(new Instant(0), 1001, 0) == new HighPrecisionTime(new Instant(1), 1, 0)}</li>
   * <li>{@code new HighPrecisionTime(new Instant(0), 0, 1001) == new HighPrecisionTime(new Instant(0), 1, 1)}</li>
   * </ul>
   *
   * @param initialTime initial timestamp
   * @param microsToAdd microseconds to add to the initial timestamp
   * @param nanosToAdd  nanoseconds to add to the initial timestamp
   */
  public HighPrecisionTime(Instant initialTime, int microsToAdd, int nanosToAdd) {
    requireNonNull(initialTime);
    int microsOverflow = microsToAdd + nanosToAdd / 1000;
    this.truncated = initialTime.plus(microsOverflow / 1000);
    this.nanosToAdd = (microsOverflow % 1000) * 1000 + nanosToAdd % 1000;
  }

  /**
   * Returns an instance of {@link Instant} truncating micro and nanoseconds.
   *
   * @return trucated timestamp
   */
  public Instant toInstant() {
    return truncated;
  }

  /**
   * Returns the number of milliseconds from Java's Eposh.
   * Truncates nanoseconds, if any.
   *
   * @return number of milliseconds
   * @see HighPrecisionTime#HighPrecisionTime(Instant, int, int)
   */
  public long toMillis() {
    return truncated.getMillis();
  }

  /**
   * Returns the number of microseconds remaining to be added to the underlying timestamp.
   * Truncates nanoseconds, if any.
   *
   * @return number of microseconds
   * @see HighPrecisionTime#HighPrecisionTime(Instant, int, int)
   */
  public int remainingMicros() {
    return nanosToAdd / 1000;
  }

  /**
   * Returns the number of nanoseconds remaining to be added to the underlying timestamp.
   *
   * @return number of nanoseconds
   * @see HighPrecisionTime#HighPrecisionTime(Instant, int, int)
   */
  public int remainingNanos() {
    return nanosToAdd;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HighPrecisionTime))
      return false;

    HighPrecisionTime other = (HighPrecisionTime) obj;
    return this.truncated.equals(other.truncated)
      && this.nanosToAdd == other.nanosToAdd;
  }

  @Override
  public int hashCode() {
    return hash(truncated, nanosToAdd);
  }

  @Override
  public String toString() {
    return format(
      "%s%06dZ",
      truncated.toString(ISODateTimeFormat.dateHourMinuteSecondMillis()),
      nanosToAdd
    );
  }
}
