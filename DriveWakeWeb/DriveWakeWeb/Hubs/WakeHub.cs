using Microsoft.AspNet.SignalR;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;

namespace DriveWakeWeb.Hubs
{
    public class WakeHub : Hub
    {
        public void pulse (int intensity) {
            Clients.All.pulseClient(intensity);
        }
    }
}