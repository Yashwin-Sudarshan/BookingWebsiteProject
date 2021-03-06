import React, { Component } from "react";
import { Row, Col } from "react-bootstrap";
import Spinner from "react-bootstrap/Spinner";
import PropTypes from "prop-types";
import { format, subHours } from "date-fns";

class PastBookingsRows extends Component {
  renderPastBookings() {
    if (!this.props.loading && this.props.pastBookings.length > 0) {
      return (
        <div>
          {this.props.pastBookings.map((pastBooking) => (
            <Row>
              <Col>{pastBooking.product.name}</Col>
              <Col>{pastBooking.staff_member.name}</Col>
              <Col>
                {format(
                  subHours(new Date(pastBooking.appointment_time), 11),
                  "d/MM/yyyy"
                )}
              </Col>
              <Col>
                {format(
                  subHours(new Date(pastBooking.appointment_time), 11),
                  "h:mm a"
                )}
              </Col>
              <br />
              <br />
            </Row>
          ))}
        </div>
      );
    } else if (!this.props.loading) {
      return <p>No bookings fulfilled as of yet</p>;
    } else {
      return <Spinner animation="border" role="status"></Spinner>;
    }
  }

  render() {
    return <div>{this.renderPastBookings()}</div>;
  }
}

PastBookingsRows.propTypes = {
  pastBookings: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      status: PropTypes.string,
      staff_member: PropTypes.arrayOf(
        PropTypes.shape({
          id: PropTypes.string,
          name: PropTypes.string,
        })
      ),
      product: PropTypes.arrayOf(
        PropTypes.shape({
          id: PropTypes.string,
          name: PropTypes.string,
          duration: PropTypes.string,
        })
      ),
      appointment_time: PropTypes.string,
    })
  ),
  loading: PropTypes.bool,
};

export default PastBookingsRows;
