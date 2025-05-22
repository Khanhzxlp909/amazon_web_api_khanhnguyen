import { createRouter, createWebHistory } from 'vue-router';
import AmazonOrderPage from '../components/AmazonOrderPage.vue'; // Updated import

const routes = [
    {
        path: '/order',
        name: 'amazonOrderPage', // Updated route name
        component: AmazonOrderPage, // Updated component reference
    },
    // Add a fallback route if needed
    {
        path: '/:pathMatch(.*)*',
        redirect: '/order',
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;

