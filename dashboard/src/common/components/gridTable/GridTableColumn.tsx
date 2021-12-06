export type GridColumnType = 'checkbox' | 'select' | 'date' | 'number' | 'elements'
export type GridTableCellValue = string | number | boolean | Date | JSX.Element[] | undefined

export interface GridTableColumnParams {
  name: string,
  headerName?: string,
  className?: string,
  width?: number,
  type?: GridColumnType,
  editable?: boolean,
  validate?: (value: GridTableCellValue, rowNum: number|undefined) => boolean,
  elements?: JSX.Element[]
}
