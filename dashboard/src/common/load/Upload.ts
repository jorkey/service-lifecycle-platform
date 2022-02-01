import axios from "axios"

export function upload(path: string, file: File): Promise<any> {
  const formData = new FormData()

  formData.append("file", file)

  const http = axios.create({
    headers: {
      Authorization: 'Bearer ' + localStorage.getItem('accessToken')
    }
  })

  return http.post(path, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
    onUploadProgress: undefined
  })
}
