package faunadb

import cats.syntax.option._

class Account { /* ... */ }

trait AccountService {
  def getAccountById(id: Int): Option[Account]
}

class DummyAccountServiceImpl extends AccountService {

  def dummyAccount: Account = ???


  /* ... */
  override def getAccountById(id: Int): Option[Account] = dummyAccount.some
  /* ... */
}