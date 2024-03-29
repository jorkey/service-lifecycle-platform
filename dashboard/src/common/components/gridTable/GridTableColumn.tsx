export type GridColumnType = 'checkbox' | 'select' | 'time' | 'relativeTime' | 'number' | 'elements' | 'upload'
export type GridTableCellValue = string | number | boolean | Date | JSX.Element[] | File | undefined

export interface GridTableColumnParams {
  name: string,
  headerName?: string,
  className?: string,
  type?: GridColumnType,
  editable?: boolean,
  validate?: (value: GridTableCellValue, rowNum: number|undefined) => boolean,
  auto?:  (values: Map<string, GridTableCellValue>) => GridTableCellValue,
  elements?: JSX.Element[]
}
