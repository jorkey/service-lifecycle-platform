
export class Utils {
  static logout() {
    localStorage.removeItem('token')
  }

  static setDistributionName(distributionName: string) {
    localStorage.setItem('distributionName', distributionName)
  }

  static getDistributionName(): string {
    return localStorage.getItem('distributionName') || ''
  }
}