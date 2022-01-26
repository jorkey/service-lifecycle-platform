import {GraphQLScalarType} from "graphql";

export class DateTime extends GraphQLScalarType {
  name = 'Date'
  description = 'A date and time, represented as an ISO-8601 string'
  serialize = (value: Date) => value.toISOString()
  parseValue = (value: string) => new Date(value)
}