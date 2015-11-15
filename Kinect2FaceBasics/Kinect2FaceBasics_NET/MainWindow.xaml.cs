using Microsoft.AspNet.SignalR.Client;
using Microsoft.Kinect;
using Microsoft.Kinect.Face;
using System;
using System.Collections.Generic; 
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Threading;

namespace Kinect2FaceBasics_NET
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        KinectSensor _sensor = null;
        ColorFrameReader _colorReader = null;
        BodyFrameReader _bodyReader = null;
        IList<Body> _bodies = null;
        IHubProxy stockTickerHubProxy;
        // 1) Specify a face frame source and a face frame reader
        FaceFrameSource _faceSource = null;
        FaceFrameReader _faceReader = null;
        int eyeclosecounter = 0;
        int eyeopencounter = 0;

        public MainWindow()
        {
            InitializeComponent();
            var hubConnection = new HubConnection("http://divewakeweb.azurewebsites.net/");
            stockTickerHubProxy = hubConnection.CreateHubProxy("WakeHub");
            hubConnection.Start().Wait();
            _sensor = KinectSensor.GetDefault();

            if (_sensor != null)
            {
                _sensor.Open();

                _bodies = new Body[_sensor.BodyFrameSource.BodyCount];

                _colorReader = _sensor.ColorFrameSource.OpenReader();
                _colorReader.FrameArrived += ColorReader_FrameArrived;
                _bodyReader = _sensor.BodyFrameSource.OpenReader();
                _bodyReader.FrameArrived += BodyReader_FrameArrived;

                // 2) Initialize the face source with the desired features
                _faceSource = new FaceFrameSource(_sensor, 0, FaceFrameFeatures.BoundingBoxInColorSpace |
                                                              FaceFrameFeatures.FaceEngagement |
                                                              FaceFrameFeatures.Glasses |
                                                              FaceFrameFeatures.LeftEyeClosed |
                                                              FaceFrameFeatures.PointsInColorSpace |
                                                              FaceFrameFeatures.RightEyeClosed);
                _faceReader = _faceSource.OpenReader();
                _faceReader.FrameArrived += FaceReader_FrameArrived;
            }
        }

        void ColorReader_FrameArrived(object sender, ColorFrameArrivedEventArgs e)
        {
            using (var frame = e.FrameReference.AcquireFrame())
            {
                if (frame != null)
                {
                    camera.Source = frame.ToBitmap();
                }
            }
        }

        void BodyReader_FrameArrived(object sender, BodyFrameArrivedEventArgs e)
        {
            using (var frame = e.FrameReference.AcquireFrame())
            {
                if (frame != null)
                {
                    frame.GetAndRefreshBodyData(_bodies);

                    Body body = _bodies.Where(b => b.IsTracked).FirstOrDefault();

                    if (!_faceSource.IsTrackingIdValid)
                    {
                        if (body != null)
                        {
                            // 4) Assign a tracking ID to the face source
                            _faceSource.TrackingId = body.TrackingId;
                        }
                    }
                }
            }
        }

        void FaceReader_FrameArrived(object sender, FaceFrameArrivedEventArgs e)
        {
            using (var frame = e.FrameReference.AcquireFrame())
            {
                if (frame != null)
                {
                    // 4) Get the face frame result
                    FaceFrameResult result = frame.FaceFrameResult;

                    if (result != null)
                    {
                        // 5) Do magic!
                        // Get the face points, mapped in the color space.
                        var eyeLeft = result.FacePointsInColorSpace[FacePointType.EyeLeft];
                        var eyeRight = result.FacePointsInColorSpace[FacePointType.EyeRight];

                        // var nose = result.FacePointsInColorSpace[FacePointType.Nose];
                        //var mouthLeft = result.FacePointsInColorSpace[FacePointType.MouthCornerLeft];
                        //var mouthRight = result.FacePointsInColorSpace[FacePointType.MouthCornerRight];

                        var eyeLeftClosed = result.FaceProperties[FaceProperty.LeftEyeClosed];
                        var eyeRightClosed = result.FaceProperties[FaceProperty.RightEyeClosed];
                        //var mouthOpen = result.FaceProperties[FaceProperty.MouthOpen];

                        // Position the canvas UI elements
                        Canvas.SetLeft(ellipseEyeLeft, eyeLeft.X - ellipseEyeLeft.Width / 2.0);
                        Canvas.SetTop(ellipseEyeLeft, eyeLeft.Y - ellipseEyeLeft.Height / 2.0);

                        Canvas.SetLeft(ellipseEyeRight, eyeRight.X - ellipseEyeRight.Width / 2.0);
                        Canvas.SetTop(ellipseEyeRight, eyeRight.Y - ellipseEyeRight.Height / 2.0);

                        //Canvas.SetLeft(ellipseNose, nose.X - ellipseNose.Width / 2.0);
                        //Canvas.SetTop(ellipseNose, nose.Y - ellipseNose.Height / 2.0);

                        //Canvas.SetLeft(ellipseMouth, ((mouthRight.X + mouthLeft.X) / 2.0) - ellipseMouth.Width / 2.0);
                        //Canvas.SetTop(ellipseMouth, ((mouthRight.Y + mouthLeft.Y) / 2.0) - ellipseMouth.Height / 2.0);
                        //ellipseMouth.Width = Math.Abs(mouthRight.X - mouthLeft.X);

                        // Display or hide the ellipses
                        if ((eyeLeftClosed == DetectionResult.Yes || eyeLeftClosed == DetectionResult.Maybe) && (eyeRightClosed == DetectionResult.Yes || eyeRightClosed == DetectionResult.Maybe))
                        {
                            /*while ((eyeLeftClosed == DetectionResult.Yes || eyeLeftClosed == DetectionResult.Maybe) && (eyeRightClosed == DetectionResult.Yes || eyeRightClosed == DetectionResult.Maybe))
                            {
                                if (counter >= 3)
                                {
                                    Sendpulse(4);
                                    break;
                                }
                                ellipseEyeLeft.Visibility = Visibility.Collapsed;
                                ellipseEyeRight.Visibility = Visibility.Collapsed;
                                counter++;
                                Thread.Sleep(1000);
                            }*/
                            eyeclosecounter++;
                            eyeopencounter = 0;
                            Sendpulse(0);                        }
                        else
                        {
                            ellipseEyeLeft.Visibility = Visibility.Visible;
                            ellipseEyeRight.Visibility = Visibility.Visible;
                            Sendpulse(1);
                            eyeopencounter++;
                            eyeclosecounter = 0;
                            //Thread.Sleep(1000);
                        }

                        if (eyeclosecounter > 30) 
                            Sendpulse(2222);
                        if (eyeopencounter > 60)
                            Sendpulse(3333);

                      /*  if (counter == 2)
                            Sendpulse(0);
                        else if (counter == 5)
                            Sendpulse(1);
                        else if (counter == 10)
                            Sendpulse(2);
                        else
                            Sendpulse(3); */

                        /*if (mouthOpen == DetectionResult.Yes || mouthOpen == DetectionResult.Maybe)
                        {
                            ellipseMouth.Height = 50.0;
                        }
                        else
                        {
                            ellipseMouth.Height = 20.0;
                        }*/
                    }
                }
            }
        }

        void Sendpulse(int intensity)
        {
            stockTickerHubProxy.Invoke("Pulse", intensity);
        }
        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            if (_colorReader != null)
            {
                _colorReader.Dispose();
                _colorReader = null;
            }

            if (_bodyReader != null)
            {
                _bodyReader.Dispose();
                _bodyReader = null;
            }

            if (_faceReader != null)
            {
                _faceReader.Dispose();
                _faceReader = null;
            }

            if (_faceSource != null)
            {
                _faceSource.Dispose();
                _faceSource = null;
            }

            if (_sensor != null)
            {
                _sensor.Close();
            }
        }
    }
}
