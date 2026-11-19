package com.example.customers.application.port.out.dto;

public sealed interface FieldUpdate<T> {

  record Keep<T>() implements FieldUpdate<T> {
  }

  record Replace<T>(T value) implements FieldUpdate<T> {
  }
}
