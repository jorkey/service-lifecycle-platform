
interface ServiceLogsParams {
  service: string
  instance: string
  process: string
  directory: string
}

interface TaskLogsParams {
  task: string
}

interface LogsGetter {
  getLogs: (params: ServiceLogsParams | TaskLogsParams) => void
  subscribeLogs: (fromSequence: number) => void
}