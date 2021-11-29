export type GridTableCellValue = string | number | boolean | Date | JSX.Element[] | undefined

export interface GridTableCellParams {
  value: GridTableCellValue,
  className?: string,
  editable?: boolean,
  select?: string[]
}

export interface GridTableColumnParams {
  name: string,
  headerName?: string,
  className?: string,
  type?: 'checkbox' | 'select' | 'date' | 'number' | 'elements',
  select?: string[],
  editable?: boolean,
  validate?: (value: GridTableCellValue, rowNum: number|undefined) => boolean,
  elements?: JSX.Element[]
}
