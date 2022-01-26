export type GridColumnType = 'checkbox' | 'select' | 'date' | 'number' | 'elements' | 'upload'
export type GridTableCellValue = string | number | boolean | Date | JSX.Element[] | File | undefined

export interface GridTableColumnParams {
  name: string,
  headerName?: string,
  className?: string,
  type?: GridColumnType,
  editable?: boolean,
  validate?: (value: GridTableCellValue, rowNum: number|undefined) => boolean,
  elements?: JSX.Element[]
}
