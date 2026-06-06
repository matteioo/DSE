import { createRouter, createWebHistory } from 'vue-router'
import SimulationView from '../views/SimulationView.vue'
import ScenarioView from '../views/ScenarioView.vue'
import AboutView from '../views/AboutView.vue'
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
    {
      path: '/about',
      name: 'about',
      component: AboutView,
    },
  ],
})

export default router
