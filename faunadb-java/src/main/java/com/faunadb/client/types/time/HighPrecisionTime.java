package com.faunadb.client.types.time;

import org.joda.time.Instant;
import org.joda.time.format.ISODateTimeFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

/**
 * Wraps an instance of {@link Instant} and adds micro nano second precision to it.
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
        toLong(precision.group("micros")),
        toLong(precision.group("nanos"))
      );
    }

    return new HighPrecisionTime(initialTime, 0, 0);
  }

  private static long toLong(String value) {
    return value != null ? Long.valueOf(value) : 0;
  }

  private final Instant truncated;
  private final long microsToAdd;
  private final long nanosToAdd;

  /**
   * Creates a new instance of {@link HighPrecisionTime}. Nano and micro seconds overflows will
   * be calculated and added to the initial timestamp.
   * <p>
   * For example:
   * <ul>
   *   <li>{@code new HighPrecisionTime(new Instant(0), 1001, 0) == new HighPrecisionTime(new Instant(1), 1, 0)}</li>
   *   <li>{@code new HighPrecisionTime(new Instant(0), 0, 1001) == new HighPrecisionTime(new Instant(0), 1, 1)}</li>
   * </ul>
   *
   * @param initialTime initial timestamp
   * @param microsToAdd micro seconds to add to the initial timestamp
   * @param nanosToAdd  nano seconds to add to the initial timestamp
   */
  public HighPrecisionTime(Instant initialTime, long microsToAdd, long nanosToAdd) {
    requireNonNull(initialTime);
    long microsOverflow = microsToAdd + nanosToAdd / 1000;

    this.truncated = initialTime.plus(microsOverflow / 1000);
    this.microsToAdd = microsOverflow % 1000;
    this.nanosToAdd = nanosToAdd % 1000;
  }

  /**
   * Returns an instance of {@link Instant} without micro and nano seconds.
   *
   * @return trucated timestamp
   */
  public Instant toInstant() {
    return truncated;
  }

  /**
   * Returns the number of micro seconds remaining to be added to the underlying timestamp.
   *
   * @return number of micro seconds
   * @see HighPrecisionTime#HighPrecisionTime(Instant, long, long)
   */
  public long getMicros() {
    return microsToAdd;
  }

  /**
   * Returns the number of nano seconds remaining to be added to the underlying timestamp.
   *
   * @return number of nano seconds
   * @see HighPrecisionTime#HighPrecisionTime(Instant, long, long)
   */
  public long getNamos() {
    return nanosToAdd;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HighPrecisionTime))
      return false;

    HighPrecisionTime other = (HighPrecisionTime) obj;
    return this.truncated.equals(other.truncated)
      && this.microsToAdd == other.microsToAdd
      && this.nanosToAdd == other.nanosToAdd;
  }

  @Override
  public int hashCode() {
    return hash(truncated, microsToAdd, nanosToAdd);
  }

  @Override
  public String toString() {
    return format(
      "%s%03d%03dZ",
      truncated.toString(ISODateTimeFormat.dateHourMinuteSecondMillis()),
      microsToAdd,
      nanosToAdd
    );
  }
}
