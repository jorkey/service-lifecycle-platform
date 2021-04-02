import {GraphQLScalarType, Kind} from 'graphql';
import {Version} from '../common/Version';

// const developerVersionScalar = new GraphQLScalarType({
//   name: 'DeveloperVersion',
//   description: 'Developer version scalar type',
//
//   serialize(value) {
//     return value.toString();
//   },
//
//   parseValue(value) {
//     return Version.DeveloperVersion.parse(value);
//   },
//
//   parseLiteral(ast) {
//     return ast.kind === Kind.STRING ? parseValue(ast.value) : null;
//   },
// });
//
// export const Scalars = {
//   developerVersionScalar
// }
