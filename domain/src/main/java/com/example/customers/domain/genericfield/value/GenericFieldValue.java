package com.example.customers.domain.genericfield.value;

public sealed interface GenericFieldValue permits BooleanValue, StringValue, IntegerValue, DatetimeValue {
}
