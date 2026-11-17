package com.example.customers.application.port;

public sealed interface FieldChange<T> {

  record Keep<T>() implements FieldChange<T> {
  }

  record Replace<T>(T value) implements FieldChange<T> {
  }

  record Clear<T>() implements FieldChange<T> {
  }
}
