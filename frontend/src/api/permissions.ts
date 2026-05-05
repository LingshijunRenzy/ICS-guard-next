import { get, post, put, del } from './client'
import type { PermissionResponse, PermissionTypeResponse } from './types'

export const permissionApi = {
    /**
     * List all permissions
     */
    list: () => get<PermissionResponse[]>('/permissions'),

    /**
     * Create a new permission
     */
    create: (data: {
        name: string
        description?: string
        metadata?: Record<string, string>
        typeId?: number
    }) => post<PermissionResponse>('/permissions', data),

    /**
     * Update an existing permission
     */
    update: (
        id: number,
        data: {
            name: string
            description?: string
            metadata?: Record<string, string>
            typeId?: number
        },
    ) => put<PermissionResponse>(`/permissions/${id}`, data),

    /**
     * Delete a permission
     */
    delete: (id: number) => del<void>(`/permissions/${id}`),

    /**
     * List all permission types
     */
    listTypes: () => get<PermissionTypeResponse[]>('/permission-types'),

    /**
     * Get a permission type by ID
     */
    getType: (id: number) => get<PermissionTypeResponse>(`/permission-types/${id}`),

    /**
     * Create a new permission type
     */
    createType: (data: { name: string; description?: string }) =>
        post<PermissionTypeResponse>('/permission-types', data),

    /**
     * Update an existing permission type
     */
    updateType: (id: number, data: { name?: string; description?: string }) =>
        put<PermissionTypeResponse>(`/permission-types/${id}`, data),

    /**
     * Delete a permission type
     */
    deleteType: (id: number) => del<void>(`/permission-types/${id}`),
}

