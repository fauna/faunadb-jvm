package com.faunadb.client.types.time;

import org.joda.time.Instant;
import org.joda.time.format.ISODateTimeFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

public class HighPrecisionTime {

  private static final Pattern PRECISION_GROUPS = Pattern.compile("\\.\\d{3}(?<micros>\\d{3})(?<nanos>\\d{3})?Z");

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

  public HighPrecisionTime(Instant initialTime, long microsToAdd, long nanosToAdd) {
    requireNonNull(initialTime);
    long microsOverflow = microsToAdd + nanosToAdd / 1000;

    this.truncated = initialTime.plus(microsOverflow / 1000);
    this.microsToAdd = microsOverflow % 1000;
    this.nanosToAdd = nanosToAdd % 1000;
  }

  public Instant truncated() {
    return truncated;
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
