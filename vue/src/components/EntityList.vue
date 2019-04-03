<template>
    <!-- TODO: the v-fors need IDs -->
    <table>
        <tr>
            <th v-for="column in columns">{{ column }}</th>
        </tr>
        <tr v-for="entity in entities">
            <td v-for="column in columns">
                <router-link :to="{name:'customer', params:{id:entity[column]}}" v-if="column === linkColumn">{{ entity[column] }}</router-link>
                <template v-else>{{ entity[column] }}</template>
            </td>
        </tr>
    </table>
</template>

<script>
    export default {
        name: "EntityList",
        props: {
            columns: Array,
            entities: Array,
            linkColumn: String,
        },
    }
</script>
