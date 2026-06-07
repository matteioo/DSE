import { createRouter, createWebHistory } from 'vue-router'
import SimulationView from '../views/SimulationView.vue'
import ScenarioView from '../views/ScenarioView.vue'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'simulation',
      component: SimulationView,
    },
    {
      path: '/scenario',
      name: 'scenario',
      component: ScenarioView,
    },
  ],
})

export default router
