
export class Utils {
  static logout() {
    localStorage.removeItem('token')
  }
}