
export function download(fileUrl: string, fileName: string) {
  const link = document.createElement("a")
  link.href = fileUrl
  link.setAttribute("download", fileName)
  link.click()
}

