package com.faunadb.client.types.time;

import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

public class HighPrecisionTime {

  private static final Pattern PRECISION_GROUPS = Pattern.compile("\\.\\d{3}(?<micros>\\d{3})(?<nanos>\\d{3})?Z");
  private static final DateTimeFormatter TIME_PARSER = ISODateTimeFormat.dateTimeParser();
  private static final DateTimeFormatter TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();

  public static HighPrecisionTime parse(String value) {
    Instant initialTime = TIME_PARSER.parseDateTime(value).toInstant();
    Matcher precision = PRECISION_GROUPS.matcher(value);

    if (precision.find()) {
      String micros = precision.group("micros");
      String nanos = precision.group("nanos");

      return new HighPrecisionTime(initialTime, Long.valueOf(micros), nanos != null ? Long.valueOf(nanos) : 0);
    }

    return new HighPrecisionTime(initialTime, 0, 0);
  }

  public static HighPrecisionTime microSeconds(long micro) {
    return new HighPrecisionTime(new Instant(0), micro, 0);
  }

  public static HighPrecisionTime nanoSeconds(long nano) {
    return new HighPrecisionTime(new Instant(0), 0, nano);
  }

  private final Instant initialTime;
  private final long microsToAdd;
  private final long nanosToAdd;

  public HighPrecisionTime(Instant initialTime, long microsToAdd, long nanosToAdd) {
    this.initialTime = requireNonNull(initialTime);
    this.microsToAdd = microsToAdd;
    this.nanosToAdd = nanosToAdd;
  }

  public Instant truncated() {
    return initialTime;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HighPrecisionTime))
      return false;

    HighPrecisionTime other = (HighPrecisionTime) obj;
    return this.initialTime.equals(other.initialTime)
      && this.microsToAdd == other.microsToAdd
      && this.nanosToAdd == other.nanosToAdd;
  }

  @Override
  public int hashCode() {
    return hash(initialTime, microsToAdd, nanosToAdd);
  }

  @Override
  public String toString() {
    return initialTime.toString(TIME_FORMAT);
  }
}
