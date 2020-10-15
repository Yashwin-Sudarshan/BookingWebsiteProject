import React, { Component } from 'react'
import {Jumbotron, Container, Row, Col} from 'react-bootstrap'
import Card from 'react-bootstrap/Card'
import PastBookingsRows from '../layouts/PastBookingsRows';
import UserProfile from "../../UserProfile.js";

class AdminPastBookingsPage extends Component {

    constructor(props){
        super(props);
        this.state={
            user_id: UserProfile.getUID(),
            pastBookings:[],
            loadingPastBookings: true,
        }
    }

    componentDidMount(){

        this.fetchPastBookings();
    }

    fetchPastBookings(){
        fetch(process.env.REACT_APP_API_URL + "/bookings?user=" + this.state.user_id + "&status=completed", {
            headers : {
                Authorization: UserProfile.getToken()
            }
        })
        .then((response) => response.json())
        .then((data) => 
        
            this.setState({pastBookings: data["bookings"], loadingPastBookings: false})
        );
    }

    render() {
        return (
            <Jumbotron id="jumbotron-cus-book-page">
                <Container>
                    <h2 className="h2-cus-book-page">Booking History</h2>
                </Container>
                <Container className="adminBookingPageContainer">
                    <Card className="shadow p-3 mb-5 bg-white rounded"  border="light" style={{ width: '68rem' }}>
                        <Card.Header as="h6">
                            <Row>
                                <Col>Service</Col>
                                <Col>Employee</Col>
                                <Col>Date</Col>
                                <Col>Time</Col>
                            </Row>
                        </Card.Header>
                        <Card.Body>
                            <PastBookingsRows
                                pastBookings={this.state.pastBookings}
                                loading={this.state.loadingPastBookings}
                            />
                        </Card.Body>
                    </Card>
                </Container>
            </Jumbotron>
        )
    }
}

export default AdminPastBookingsPage;