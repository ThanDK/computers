import React from 'react';
import { Table, Spinner, Button, Alert } from 'react-bootstrap';
import {
    useReactTable,
    getCoreRowModel,
    getFilteredRowModel,
    getPaginationRowModel,
    getSortedRowModel,
    flexRender
} from '@tanstack/react-table';

const ReusableTable = ({
    data,
    columns,
    isLoading,
    error,
    sorting,
    setSorting,
    columnFilters,
    setColumnFilters,
    globalFilter,
    setGlobalFilter,
    pagination,
    onPaginationChange,
    keepPageOnDataUpdate = false
}) => {
    
    const table = useReactTable({
        data,
        columns,
        state: {
            sorting,
            columnFilters,
            globalFilter,
            pagination,
        },
        onPaginationChange: onPaginationChange, 
        
        autoResetPageIndex: !keepPageOnDataUpdate,
        
        onSortingChange: setSorting,
        onColumnFiltersChange: setColumnFilters,
        onGlobalFilterChange: setGlobalFilter,
        getCoreRowModel: getCoreRowModel(),
        getSortedRowModel: getSortedRowModel(),
        getFilteredRowModel: getFilteredRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
    });

    return (
        <>
            <div className="table-container">
                <Table striped hover responsive variant="dark" className="custom-table">
                    <thead>
                        {table.getHeaderGroups().map(headerGroup => (
                            <tr key={headerGroup.id}>
                                {headerGroup.headers.map(header => (
                                    <th 
                                        key={header.id} 
                                        style={{ width: header.column.columnDef.meta?.width ?? 'auto' }} 
                                        className={header.column.columnDef.meta?.cellClassName}
                                        onClick={header.column.getToggleSortingHandler()}
                                    >
                                        {header.isPlaceholder
                                            ? null
                                            : flexRender(header.column.columnDef.header, header.getContext())}
                                        {{ asc: ' ▲', desc: ' ▼' }[header.column.getIsSorted()] ?? ''}
                                    </th>
                                ))}
                            </tr>
                        ))}
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr>
                                <td colSpan={columns.length} className="text-center p-5">
                                    <Spinner animation="border" />
                                </td>
                            </tr>
                        ) : error ? (
                             <tr>
                                <td colSpan={columns.length}>
                                    <Alert variant="danger" className="m-4">{error}</Alert>
                                </td>
                            </tr>
                        ) : table.getRowModel().rows.length > 0 ? (
                            table.getRowModel().rows.map(row => (
                                <tr key={row.id}>
                                    {row.getVisibleCells().map(cell => (
                                        <td key={cell.id} className={cell.column.columnDef.meta?.cellClassName}>
                                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                        </td>
                                    ))}
                                </tr>
                            ))
                        ) : (
                             <tr>
                                <td colSpan={columns.length} className="text-center p-4">
                                    No results found.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </Table>
            </div>

            <div className="d-flex justify-content-between align-items-center mt-3 flex-wrap gap-2">
                <div className="d-flex align-items-center gap-2">
                    <span className="text-secondary">Rows per page:</span>
                    <select
                        className="form-select form-select-sm"
                        style={{ width: '75px', backgroundColor: 'var(--primary-bg)', color: 'var(--text-primary)', border: '1px solid #4a5a76' }}
                        value={table.getState().pagination.pageSize}
                        onChange={e => {
                            table.setPageSize(Number(e.target.value))
                        }}
                    >
                        {[10, 20, 30, 40, 50].map(pageSize => (
                            <option key={pageSize} value={pageSize}>
                                {pageSize}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="d-flex align-items-center gap-2">
                    <Button className="pagination-btn" variant="outline-light" onClick={() => table.setPageIndex(0)} disabled={!table.getCanPreviousPage()}>{'<<'}</Button>
                    <Button className="pagination-btn" variant="outline-light" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>Previous</Button>
                    <span className="mx-2">Page{' '}
                        <strong>
                            {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
                        </strong>
                    </span>
                    <Button className="pagination-btn" variant="outline-light" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>Next</Button>
                    <Button className="pagination-btn" variant="outline-light" onClick={() => table.setPageIndex(table.getPageCount() - 1)} disabled={!table.getCanNextPage()}>{'>>'}</Button>
                </div>
            </div>
        </>
    );
};

export default ReusableTable;