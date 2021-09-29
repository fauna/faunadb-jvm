package com.faunadb.client.errors;

import java.util.List;

public class TransactionAbortedException extends FaunaException {

  public TransactionAbortedException(final String message, final int httpStatusCode, final List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.TRANSACTION_ABORTED;
  }
}
