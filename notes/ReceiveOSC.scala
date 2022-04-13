val rc = osc.UDP.Config()
rc.localIsLoopback = true
rc.localPort = 57130
val r = osc.UDP.Receiver(rc)
r.connect()
r.dump()
