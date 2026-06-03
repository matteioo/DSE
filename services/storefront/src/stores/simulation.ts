import { ref } from 'vue'
import { defineStore } from 'pinia'
import { spiderApi } from '@/api/spiderApi'
import type { VehicleState, EventLogEntry } from '@/types/simulation'

export const useSimulationStore = defineStore('simulation', () => {
  const vehicles = ref<VehicleState[]>([])

  const vehicleDetails = ref<Record<string, VehicleState>>({})

  const events = ref<EventLogEntry[]>([])

  const loading = ref(false)
  const error = ref<string | null>(null)

  let pollTimer: ReturnType<typeof setInterval> | null = null

  async function fetchData() {
    loading.value = true
    try {
      const [vehicleList, evts] = await Promise.all([
        spiderApi.getVehicles(),
        spiderApi.getEvents(),
      ])

      const details = await Promise.all(
        vehicleList.map((v) => spiderApi.getVehicle(v.vin)),
      )

      vehicles.value = vehicleList
      events.value = evts
      vehicleDetails.value = Object.fromEntries(
        vehicleList.map((v, i) => [v.vin, details[i]]),
      )

      error.value = null
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to reach Spider'
    } finally {
      loading.value = false
    }
  }

  function startPolling(intervalMs = 1000) {
    fetchData()
    pollTimer = setInterval(fetchData, intervalMs)
  }

  function stopPolling() {
    if (pollTimer !== null) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  return { vehicles, vehicleDetails, events, loading, error, startPolling, stopPolling }
})
