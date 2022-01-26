export function stripProperty(obj: any, propToDelete: string) {
  for (const property in obj) {
    if (typeof obj[property] === 'object' && !(obj[property] instanceof File)) {
      delete obj.property;
      const newData = stripProperty(obj[property], propToDelete);
      obj[property] = newData;
    } else {
      if (property === propToDelete) {
        delete obj[property];
      }
    }
  }
  return obj;
}
