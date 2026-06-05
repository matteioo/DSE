import type { VehicleState, EventLogEntry } from '@/types/simulation'

const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`)
  if (!res.ok) throw new Error(`Spider API ${path} returned ${res.status}`)
  return res.json() as Promise<T>
}

export const spiderApi = {
  getVehicles: (): Promise<VehicleState[]> =>
    get('/api/storefront/vehicles'),

  getVehicle: (vin: string): Promise<VehicleState> =>
    get(`/api/storefront/vehicles/${encodeURIComponent(vin)}`),

  getEvents: (): Promise<EventLogEntry[]> =>
    get('/api/storefront/events'),
}
