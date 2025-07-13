// src/components/ReusableTable/ReusableTable.js

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

/**
 * A reusable table component built with @tanstack/react-table.
 * It handles rendering, sorting, pagination, and filtering based on props.
 *
 * @param {Array} data - The array of data to display.
 * @param {Array} columns - The column definitions for the table. See @tanstack/react-table docs.
 * @param {boolean} isLoading - If true, shows a loading spinner in the table body.
 * @param {string} error - If present, shows an error message.
 * @param {Array} sorting - The current sorting state.
 * @param {Function} setSorting - The state setter for sorting.
 * @param {Array} columnFilters - The current column filters state.
 * @param {Function} setColumnFilters - The state setter for column filters.
 * @param {string} globalFilter - The current global filter state.
 * @param {Function} setGlobalFilter - The state setter for the global filter.
 */
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
    setGlobalFilter
}) => {

    const table = useReactTable({
        data,
        columns,
        state: {
            sorting,
            columnFilters,
            globalFilter,
        },
        onSortingChange: setSorting,
        onColumnFiltersChange: setColumnFilters,
        onGlobalFilterChange: setGlobalFilter,
        getCoreRowModel: getCoreRowModel(),
        getSortedRowModel: getSortedRowModel(),
        getFilteredRowModel: getFilteredRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
    });

    // Main render logic
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
                                        style={{ 

                                            width: header.column.columnDef.meta?.width ?? 'auto' 
                                        }} 
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

            <div className="d-flex justify-content-end align-items-center mt-3 gap-2">
                <Button className="pagination-btn" variant="outline-light" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>Previous</Button>
                <span className="mx-2">Page{' '}
                    <strong>
                        {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
                    </strong>
                </span>
                <Button className="pagination-btn" variant="outline-light" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>Next</Button>
            </div>
        </>
    );
};

export default ReusableTable;