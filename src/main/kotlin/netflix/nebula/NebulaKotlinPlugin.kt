package netflix.nebula

class NebulaKotlinPlugin : NebulaBaseKotlinPlugin() {
    override fun isOnlyTestPlugin(): Boolean {
        return false
    }
}
