export interface VehicleState {
  vin: string
  role: string
  latitude: number
  longitude: number
  speedKmh: number
  distanceToFrontM: number | null
  distanceToRearM: number | null
  distanceChangeMps: number
  emergencyBrakeActive: boolean
  updatedAt: string
}

export interface EventLogEntry {
  timestamp: string
  level: string
  source: string
  message: string
}

